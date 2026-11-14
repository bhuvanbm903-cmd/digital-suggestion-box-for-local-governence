import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export const isSupabaseConfigured = () => {
  return (
    typeof supabaseUrl === 'string' &&
    supabaseUrl.trim().length > 0 &&
    supabaseUrl.startsWith('https://') &&
    typeof supabaseAnonKey === 'string' &&
    supabaseAnonKey.trim().length > 0 &&
    !supabaseUrl.includes('your-project-id')
  );
};

export const supabase = isSupabaseConfigured()
  ? createClient(supabaseUrl, supabaseAnonKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    })
  : null;

/**
 * Normalizes an Indian mobile number into 10 digits and standard +91 format.
 */
export const normalizeMobile = (raw) => {
  if (!raw) return { valid: false, cleanDigits: '', fullMobile: '' };
  const digits = raw.toString().replace(/\D/g, '');
  // Extract 10 digits (ignoring leading 91 or 0)
  let tenDigits = digits;
  if (digits.length === 12 && digits.startsWith('91')) {
    tenDigits = digits.slice(2);
  } else if (digits.length === 11 && digits.startsWith('0')) {
    tenDigits = digits.slice(1);
  }
  const valid = /^[6-9]\d{9}$/.test(tenDigits);
  return {
    valid,
    cleanDigits: tenDigits,
    fullMobile: valid ? `+91${tenDigits}` : raw,
  };
};

/**
 * Converts mobile number into a unique synthetic email for free Supabase Auth
 * without requiring paid SMS gateway credits.
 */
export const phoneToEmail = (fullMobile) => {
  const digits = fullMobile.replace(/\D/g, '');
  return `${digits}@phone.suggestbox.gov.in`;
};

// ==============================================================================
// In-Memory & LocalStorage Mock Provider (Active when Supabase keys are pending)
// ==============================================================================

const MOCK_USERS_KEY = 'suggestbox_mock_users';
const MOCK_SESSION_KEY = 'suggestbox_mock_session';
const MOCK_SUGGESTIONS_KEY = 'suggestbox_mock_suggestions';

const getInitialMockSuggestions = () => [
  {
    id: 'sug-1',
    tracking_id: 'SBX-W14-1001',
    user_id: 'mock-citizen-1',
    category: 'Roads & Footpaths',
    department: 'BBMP Engineering',
    title: 'Deep pothole on 100 Feet Road junction near metro pillar 42',
    description: 'A dangerous 2-foot pothole has emerged right after the recent monsoon rain, posing high risk of 2-wheeler skidding during night hours.',
    ward_id: 'ward-14',
    ward_name: 'Ward 14 • Indiranagar',
    status: 'In Progress',
    admin_response: 'Inspection completed by Ward Jr. Engineer. Asphalt patching crew scheduled for overnight repair on Saturday.',
    is_anonymous: false,
    created_at: new Date(Date.now() - 2 * 24 * 3600 * 1000).toISOString(),
  },
  {
    id: 'sug-2',
    tracking_id: 'SBX-W14-1002',
    user_id: 'mock-citizen-1',
    category: 'Water & Sanitation',
    department: 'BWSSB Water Supply',
    title: 'Drinking water pipeline joint leak on 6th Main',
    description: 'Clean drinking water is leaking from the underground feeder pipe near the community park gate, causing road erosion.',
    ward_id: 'ward-14',
    ward_name: 'Ward 14 • Indiranagar',
    status: 'Under Review',
    admin_response: null,
    is_anonymous: true,
    created_at: new Date(Date.now() - 4 * 24 * 3600 * 1000).toISOString(),
  },
];

export const getMockSession = () => {
  try {
    const raw = localStorage.getItem(MOCK_SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const setMockSession = (user) => {
  if (user) {
    localStorage.setItem(MOCK_SESSION_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(MOCK_SESSION_KEY);
  }
};

const getMockUsers = () => {
  try {
    const raw = localStorage.getItem(MOCK_USERS_KEY);
    if (!raw) {
      // Seed default citizen and admin demo accounts
      const defaultUsers = [
        {
          id: 'mock-citizen-1',
          full_name: 'Bhuvan Kumar',
          mobile_number: '+919845012345',
          password: 'password123',
          role: 'citizen',
          created_at: new Date().toISOString(),
        },
        {
          id: 'mock-admin-1',
          full_name: 'Municipal Commissioner',
          mobile_number: '+919880011223',
          password: 'adminpassword',
          role: 'admin',
          created_at: new Date().toISOString(),
        },
      ];
      localStorage.setItem(MOCK_USERS_KEY, JSON.stringify(defaultUsers));
      return defaultUsers;
    }
    return JSON.parse(raw);
  } catch {
    return [];
  }
};

const getMockSuggestions = () => {
  try {
    const raw = localStorage.getItem(MOCK_SUGGESTIONS_KEY);
    if (!raw) {
      const initial = getInitialMockSuggestions();
      localStorage.setItem(MOCK_SUGGESTIONS_KEY, JSON.stringify(initial));
      return initial;
    }
    return JSON.parse(raw);
  } catch {
    return getInitialMockSuggestions();
  }
};

// ==============================================================================
// Authentication Services
// ==============================================================================

/**
 * Registers a new user using their 10-digit Indian Mobile Number and Password.
 */
export const registerWithMobile = async ({ mobileNumber, password, fullName }) => {
  const { valid, fullMobile } = normalizeMobile(mobileNumber);
  if (!valid) {
    throw new Error('Please enter a valid 10-digit Indian mobile number (e.g. 9845012345).');
  }
  if (!password || password.length < 6) {
    throw new Error('Password must be at least 6 characters long.');
  }
  if (!fullName || fullName.trim().length < 2) {
    throw new Error('Please enter your full name.');
  }

  // Live Supabase Auth
  if (isSupabaseConfigured() && supabase) {
    // 1. Check if mobile number is already registered in user_profiles
    const { data: existing, error: checkError } = await supabase
      .from('user_profiles')
      .select('id')
      .eq('mobile_number', fullMobile)
      .maybeSingle();

    if (existing) {
      throw new Error('This mobile number is already registered. Please sign in instead.');
    }

    const email = phoneToEmail(fullMobile);
    const { data: authData, error: authError } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          full_name: fullName.trim(),
          mobile_number: fullMobile,
          role: 'citizen',
        },
      },
    });

    if (authError) {
      if (authError.message.includes('already registered')) {
        throw new Error('This mobile number is already registered. Please sign in.');
      }
      throw authError;
    }

    const user = authData.user;
    if (!user) {
      throw new Error('Sign up failed. Please try again.');
    }

    // 2. Ensure user_profiles row exists
    await supabase.from('user_profiles').upsert({
      id: user.id,
      full_name: fullName.trim(),
      mobile_number: fullMobile,
      role: 'citizen',
    });

    return {
      id: user.id,
      full_name: fullName.trim(),
      mobile_number: fullMobile,
      role: 'citizen',
    };
  }

  // Local Mock Mode
  const users = getMockUsers();
  if (users.some((u) => u.mobile_number === fullMobile)) {
    throw new Error('This mobile number is already registered. Please sign in instead.');
  }

  const newUser = {
    id: `usr-${Date.now()}`,
    full_name: fullName.trim(),
    mobile_number: fullMobile,
    password,
    role: 'citizen',
    created_at: new Date().toISOString(),
  };

  users.push(newUser);
  localStorage.setItem(MOCK_USERS_KEY, JSON.stringify(users));
  setMockSession(newUser);
  return newUser;
};

/**
 * Logs in an existing user using Mobile Number and Password.
 */
export const loginWithMobile = async ({ mobileNumber, password }) => {
  const { valid, fullMobile } = normalizeMobile(mobileNumber);
  if (!valid) {
    throw new Error('Please enter a valid 10-digit Indian mobile number.');
  }
  if (!password) {
    throw new Error('Please enter your password.');
  }

  // Live Supabase Auth
  if (isSupabaseConfigured() && supabase) {
    const email = phoneToEmail(fullMobile);
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (authError) {
      throw new Error('Invalid mobile number or password. Please verify and try again.');
    }

    const user = authData.user;
    // Fetch user profile
    const { data: profile } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('id', user.id)
      .maybeSingle();

    return {
      id: user.id,
      full_name: profile?.full_name || user.user_metadata?.full_name || 'Citizen Participant',
      mobile_number: profile?.mobile_number || fullMobile,
      role: profile?.role || 'citizen',
    };
  }

  // Local Mock Mode
  const users = getMockUsers();
  const match = users.find((u) => u.mobile_number === fullMobile && u.password === password);
  if (!match) {
    throw new Error('Invalid mobile number or password. Please verify and try again.');
  }

  const sessionUser = {
    id: match.id,
    full_name: match.full_name,
    mobile_number: match.mobile_number,
    role: match.role || 'citizen',
  };
  setMockSession(sessionUser);
  return sessionUser;
};

/**
 * Restores the current authenticated session on page refresh.
 */
export const getActiveSession = async () => {
  if (isSupabaseConfigured() && supabase) {
    const { data } = await supabase.auth.getSession();
    const user = data?.session?.user;
    if (!user) return null;

    const { data: profile } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('id', user.id)
      .maybeSingle();

    return {
      id: user.id,
      full_name: profile?.full_name || user.user_metadata?.full_name || 'Citizen Participant',
      mobile_number: profile?.mobile_number || user.user_metadata?.mobile_number || '',
      role: profile?.role || 'citizen',
    };
  }

  return getMockSession();
};

/**
 * Signs out the current user and clears session state.
 */
export const logoutUser = async () => {
  if (isSupabaseConfigured() && supabase) {
    await supabase.auth.signOut();
  }
  setMockSession(null);
};

// ==============================================================================
// Suggestions Data Operations (Protected by RLS)
// ==============================================================================

export const fetchSuggestions = async (userId, userRole = 'citizen') => {
  if (isSupabaseConfigured() && supabase) {
    let query = supabase.from('suggestions').select('*').order('created_at', { ascending: false });
    // If not admin, RLS automatically restricts to own, but we explicitly filter by userId for clarity
    if (userRole !== 'admin' && userRole !== 'official') {
      query = query.eq('user_id', userId);
    }
    const { data, error } = await query;
    if (error) throw error;
    return data || [];
  }

  const all = getMockSuggestions();
  if (userRole === 'admin' || userRole === 'official') {
    return all;
  }
  return all.filter((s) => s.user_id === userId);
};

export const submitSuggestion = async (suggestionData, currentUser) => {
  const trackingId = `SBX-W14-${Math.floor(1000 + Math.random() * 9000)}`;
  const record = {
    tracking_id: trackingId,
    user_id: currentUser.id,
    category: suggestionData.category,
    department: suggestionData.department,
    title: suggestionData.title.trim(),
    description: suggestionData.description.trim(),
    ward_id: 'ward-14',
    ward_name: 'Ward 14 • Indiranagar',
    status: 'Submitted',
    admin_response: null,
    is_anonymous: !!suggestionData.is_anonymous,
    created_at: new Date().toISOString(),
  };

  if (isSupabaseConfigured() && supabase) {
    const { data, error } = await supabase.from('suggestions').insert([record]).select().single();
    if (error) throw error;
    return data;
  }

  const list = getMockSuggestions();
  const created = { ...record, id: `sug-${Date.now()}` };
  list.unshift(created);
  localStorage.setItem(MOCK_SUGGESTIONS_KEY, JSON.stringify(list));
  return created;
};

export const updateSuggestionStatus = async (suggestionId, newStatus, responseText) => {
  if (isSupabaseConfigured() && supabase) {
    const { data, error } = await supabase
      .from('suggestions')
      .update({
        status: newStatus,
        admin_response: responseText,
        updated_at: new Date().toISOString(),
      })
      .eq('id', suggestionId)
      .select()
      .single();
    if (error) throw error;
    return data;
  }

  const list = getMockSuggestions();
  const index = list.findIndex((s) => s.id === suggestionId);
  if (index !== -1) {
    list[index] = {
      ...list[index],
      status: newStatus,
      admin_response: responseText,
      updated_at: new Date().toISOString(),
    };
    localStorage.setItem(MOCK_SUGGESTIONS_KEY, JSON.stringify(list));
    return list[index];
  }
  throw new Error('Suggestion not found');
};
