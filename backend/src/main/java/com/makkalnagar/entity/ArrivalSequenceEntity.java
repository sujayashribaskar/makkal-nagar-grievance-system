package com.makkalnagar.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "arrival_sequences")
public class ArrivalSequenceEntity {

    @Id
    @Column(name = "sequence_name", length = 50)
    private String sequenceName;

    @Column(name = "current_val", nullable = false)
    private Long currentVal;

    public ArrivalSequenceEntity() {}

    public ArrivalSequenceEntity(String sequenceName, Long currentVal) {
        this.sequenceName = sequenceName;
        this.currentVal = currentVal;
    }

    public String getSequenceName() { return sequenceName; }
    public void setSequenceName(String sequenceName) { this.sequenceName = sequenceName; }

    public Long getCurrentVal() { return currentVal; }
    public void setCurrentVal(Long currentVal) { this.currentVal = currentVal; }
}
