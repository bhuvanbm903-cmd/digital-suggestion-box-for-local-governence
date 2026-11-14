-- ==============================================================================
-- SuggestBox / DigitalBox - Supabase PostgreSQL Database Schema
-- Mobile Number + Password Authentication & Row Level Security (RLS)
-- ==============================================================================

-- 1. Enable UUID Extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Create User Profiles Table
-- Stores citizen identity linked to Supabase Auth (auth.users)
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    mobile_number VARCHAR(15) UNIQUE NOT NULL,
    role VARCHAR(20) DEFAULT 'citizen' CHECK (role IN ('citizen', 'official', 'admin')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast lookup on mobile number
CREATE INDEX IF NOT EXISTS idx_user_profiles_mobile ON public.user_profiles(mobile_number);

-- 3. Create Suggestions Table
-- Stores geotagged civic grievances (Fix-It) and community proposals (Build-It)
CREATE TABLE IF NOT EXISTS public.suggestions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracking_id TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    department TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    ward_id TEXT DEFAULT 'ward-14',
    ward_name TEXT DEFAULT 'Ward 14 • Indiranagar',
    status TEXT DEFAULT 'Submitted' CHECK (status IN ('Submitted', 'Under Review', 'In Progress', 'Resolved', 'Rejected')),
    admin_response TEXT,
    is_anonymous BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_suggestions_user_id ON public.suggestions(user_id);
CREATE INDEX IF NOT EXISTS idx_suggestions_status ON public.suggestions(status);
CREATE INDEX IF NOT EXISTS idx_suggestions_tracking_id ON public.suggestions(tracking_id);

-- ==============================================================================
-- 4. Row Level Security (RLS) Policies
-- Enforces privacy: Citizens view and create ONLY their own submissions.
-- Admins/Officials have full oversight.
-- ==============================================================================

-- Enable RLS on both tables
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.suggestions ENABLE ROW LEVEL SECURITY;

-- Helper function to check if current authenticated user is an admin/official
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.user_profiles
    WHERE id = auth.uid() AND role IN ('admin', 'official')
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ------------------------------------------------------------------------------
-- Policies for: user_profiles
-- ------------------------------------------------------------------------------

-- Allow users to read their own profile, or admins to read all
CREATE POLICY "Users can view own profile"
    ON public.user_profiles
    FOR SELECT
    USING (auth.uid() = id OR public.is_admin());

-- Allow users to insert their own profile during registration
CREATE POLICY "Users can insert own profile"
    ON public.user_profiles
    FOR INSERT
    WITH CHECK (auth.uid() = id);

-- Allow users to update their own profile
CREATE POLICY "Users can update own profile"
    ON public.user_profiles
    FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- ------------------------------------------------------------------------------
-- Policies for: suggestions
-- ------------------------------------------------------------------------------

-- Citizens can only view their own suggestions; Admins can view all
CREATE POLICY "Users can view own suggestions or admins view all"
    ON public.suggestions
    FOR SELECT
    USING (auth.uid() = user_id OR public.is_admin());

-- Citizens can insert their own suggestions
CREATE POLICY "Users can insert own suggestions"
    ON public.suggestions
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Users can edit their own suggestions while still 'Submitted'
CREATE POLICY "Users can update own pending suggestions"
    ON public.suggestions
    FOR UPDATE
    USING (auth.uid() = user_id AND status = 'Submitted')
    WITH CHECK (auth.uid() = user_id);

-- Admins and Officials can update any suggestion (e.g. status & admin_response)
CREATE POLICY "Admins can update suggestions"
    ON public.suggestions
    FOR UPDATE
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Users can delete their own suggestions while 'Submitted' (DPDP right to erase)
CREATE POLICY "Users can delete own pending suggestions"
    ON public.suggestions
    FOR DELETE
    USING (auth.uid() = user_id AND status = 'Submitted');

-- ==============================================================================
-- 5. Trigger: Auto-populate user_profiles from auth.users metadata on Sign Up
-- ==============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    mobile_val TEXT;
    name_val TEXT;
BEGIN
    mobile_val := COALESCE(
        new.raw_user_meta_data->>'mobile_number',
        new.raw_user_meta_data->>'phone',
        new.phone
    );
    name_val := COALESCE(
        new.raw_user_meta_data->>'full_name',
        'Citizen Participant'
    );

    IF mobile_val IS NOT NULL THEN
        INSERT INTO public.user_profiles (id, full_name, mobile_number, role)
        VALUES (new.id, name_val, mobile_val, 'citizen')
        ON CONFLICT (id) DO UPDATE
        SET full_name = EXCLUDED.full_name,
            mobile_number = EXCLUDED.mobile_number,
            updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Bind trigger to auth.users table
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ==============================================================================
-- 6. Helper RPC: Check if mobile number is already registered (Pre-check)
-- Returns true if mobile number exists, false otherwise.
-- Callable by anonymous users before registration.
-- ==============================================================================
CREATE OR REPLACE FUNCTION public.check_mobile_registered(target_mobile TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.user_profiles
        WHERE mobile_number = target_mobile
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
