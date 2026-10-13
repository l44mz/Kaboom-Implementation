package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "robots")
@NoArgsConstructor
@Getter
@Setter
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_expression")
    private Expression currentExpression = Expression.smile;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_state")
    private ConnectionState connectionState = ConnectionState.disconnected;

    public enum Expression {
        smile, eyes_closed, wink, distressed, round_eyes
    }

    public enum ConnectionState {
        connected, disconnected
    }

    public Robot(User user) {
        this.user = user;
    }

    public void updateExpression(String state) {
        if (state != null) {
            this.currentExpression = Expression.valueOf(state.toLowerCase());
        }
    }

    public boolean connect() {
        if (this.connectionState == ConnectionState.disconnected) {
            this.connectionState = ConnectionState.connected;
            return true;
        }
        return false;
    }
}