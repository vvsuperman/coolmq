package com.coolmq.amqp.util;

import org.springframework.amqp.rabbit.support.CorrelationData;

public class CompleteCorrelationData extends CorrelationData {

    private String coordinator;

    public CompleteCorrelationData(String id, String coordinator){
        super(id);
        this.coordinator = coordinator;
    }

    public String getCoordinator(){
        return this.coordinator;
    }

    @Override
    public String toString(){
        return "CompleteCorrelationData id=" + getId() +",coordinator" + this.coordinator;
    }
}
