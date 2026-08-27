package com.aditya.analytics.sink;

import com.aditya.analytics.model.TransactionEvent;

public interface AnalyticsSink {
    void write(TransactionEvent event);
}