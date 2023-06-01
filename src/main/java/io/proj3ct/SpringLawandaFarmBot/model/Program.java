package io.proj3ct.SpringLawandaFarmBot.model;

import java.sql.Date;
import java.sql.Time;

public class Program {
    private String program;
    private Date date;
    private Time time;

    public Program(){}

    public Program(String program, Date date, Time time) {
        this.program = program;
        this.date = date;
        this.time = time;
    }

    @Override
    public String toString() {
        return "Program{" +
                "program='" + program + '\'' +
                ", date=" + date +
                ", time=" + time +
                '}';
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }
}
