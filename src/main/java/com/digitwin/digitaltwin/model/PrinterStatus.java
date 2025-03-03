package com.digitwin.digitaltwin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PrinterStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean sdReady;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "text", column = @Column(name = "state_text")),
            @AttributeOverride(name = "error", column = @Column(name = "state_error"))
    })
    private State state;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bed.actual", column = @Column(name = "bed_actual")),
            @AttributeOverride(name = "bed.target", column = @Column(name = "bed_target")),
            @AttributeOverride(name = "tool0.actual", column = @Column(name = "tool0_actual")),
            @AttributeOverride(name = "tool0.target", column = @Column(name = "tool0_target"))
    })
    private Temperature temperature;

    private LocalDateTime timestamp;

    public PrinterStatus() {
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isSdReady() {
        return sdReady;
    }

    public void setSdReady(boolean sdReady) {
        this.sdReady = sdReady;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Embeddable
    public static class State {
        private String text;
        private String error;

        @Embedded
        private Flags flags;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Flags getFlags() {
            return flags;
        }

        public void setFlags(Flags flags) {
            this.flags = flags;
        }
    }

    @Embeddable
    public static class Flags {
        private boolean cancelling;
        private boolean operational;
        private boolean printing;


        private boolean paused;


        public boolean isCancelling() {
            return cancelling;
        }

        public void setCancelling(boolean cancelling) {
            this.cancelling = cancelling;
        }

        public boolean isOperational() {
            return operational;
        }

        public void setOperational(boolean operational) {
            this.operational = operational;
        }

        public boolean isPrinting() {
            return printing;
        }

        public void setPrinting(boolean printing) {
            this.printing = printing;
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }
    }

    @Embeddable
    public static class Temperature {
        @Embedded
        private Bed bed;

        @Embedded
        private Tool tool0;

        public Bed getBed() {
            return bed;
        }

        public void setBed(Bed bed) {
            this.bed = bed;
        }

        public Tool getTool0() {
            return tool0;
        }

        public void setTool0(Tool tool0) {
            this.tool0 = tool0;
        }
    }

    @Embeddable
    public static class Bed {
        private double actual;
        private double target;

        public double getActual() {
            return actual;
        }

        public void setActual(double actual) {
            this.actual = actual;
        }

        public double getTarget() {
            return target;
        }

        public void setTarget(double target) {
            this.target = target;
        }
    }

    @Embeddable
    public static class Tool {
        private double actual;
        private double target;

        public double getActual() {
            return actual;
        }

        public void setActual(double actual) {
            this.actual = actual;
        }

        public double getTarget() {
            return target;
        }

        public void setTarget(double target) {
            this.target = target;
        }
    }
}
