-- Create meeting_participants table
CREATE TABLE meeting_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,  -- Nullable for external participants
    external_email VARCHAR(255),                          -- For external participants
    external_name VARCHAR(255),                           -- For external participants
    participant_type VARCHAR(20) NOT NULL CHECK (participant_type IN ('INTERNAL', 'EXTERNAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INVITED', 'ACCEPTED', 'DECLINED')),
    
    -- Ensure either user_id OR external_email+name is provided, not both
    CONSTRAINT check_participant_data 
        CHECK (
            (participant_type = 'INTERNAL' AND user_id IS NOT NULL AND external_email IS NULL AND external_name IS NULL) OR
            (participant_type = 'EXTERNAL' AND user_id IS NULL AND external_email IS NOT NULL AND external_name IS NOT NULL)
        )
);

-- Performance indexes
CREATE INDEX idx_meeting_participants_meeting ON meeting_participants(meeting_id);
CREATE INDEX idx_meeting_participants_user ON meeting_participants(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_meeting_participants_external ON meeting_participants(external_email) WHERE external_email IS NOT NULL;

-- Prevent duplicate participants (same user or same external email per meeting)
CREATE UNIQUE INDEX idx_meeting_participants_unique_internal 
    ON meeting_participants(meeting_id, user_id) 
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX idx_meeting_participants_unique_external 
    ON meeting_participants(meeting_id, external_email) 
    WHERE external_email IS NOT NULL;
