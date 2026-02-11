-- Create time_slots table
CREATE TABLE time_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'BUSY', 'BOOKED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure end_time is after start_time
    CONSTRAINT check_time_slot_duration CHECK (end_time > start_time)
);

-- Performance indexes for common queries
CREATE INDEX idx_time_slots_user_time ON time_slots(user_id, start_time, end_time);
CREATE INDEX idx_time_slots_status_time ON time_slots(status, start_time);
CREATE INDEX idx_time_slots_user_status ON time_slots(user_id, status);

-- Prevent overlapping time slots for the same user
CREATE UNIQUE INDEX idx_time_slots_no_overlap ON time_slots(user_id, start_time, end_time);
