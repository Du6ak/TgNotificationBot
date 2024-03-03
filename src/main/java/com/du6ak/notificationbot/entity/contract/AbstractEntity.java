package com.du6ak.notificationbot.entity.contract;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

//@Entity
@Getter
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

}
