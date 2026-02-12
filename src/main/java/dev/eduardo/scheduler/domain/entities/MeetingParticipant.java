package dev.eduardo.scheduler.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "meeting_participants")
public class MeetingParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    @ToString.Exclude
    private Meeting meeting;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Nullable for external participants
    
    @Column(name = "external_email")
    private String externalEmail;  // For external participants
    
    @Column(name = "external_name")
    private String externalName;   // For external participants
    
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false)
    private ParticipantType participantType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;
    
    public enum ParticipantType {
        INTERNAL, EXTERNAL
    }
    
    public enum ParticipantStatus {
        INVITED, ACCEPTED, DECLINED
    }
}
