package com.koscom.kafkacop.util;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class DeletableBaseEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "IS_DELETED", nullable = false)
    private Boolean isDeleted = false;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        isDeleted = true;
    }

    public void restore() {
        this.deletedAt = null;
        isDeleted = false;
    }
}
