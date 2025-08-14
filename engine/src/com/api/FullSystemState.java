package com.api;

import com.program.Program;

import java.io.Serializable;
import java.util.List;

class FullSystemState implements Serializable {
    Program program;
    List<Statistic> statistics;
    int globalIndex;

    FullSystemState(Program program) {
        this.program = program;
        this.statistics = Statistic.getStatistics();
        this.globalIndex = Statistic.getGlobalIndex();
    }

    void apply(){
        Api.setCurProgram(program);
        Statistic.setGlobalIndex(globalIndex);
        Statistic.setStatistics(statistics);
    }
}
