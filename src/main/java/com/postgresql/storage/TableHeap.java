package com.postgresql.storage;

import com.postgresql.model.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TableHeap implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Tuple> tuples = new ArrayList<>();

    public void insertTuple(Tuple tuple) {
        tuples.add(tuple);
    }

    public List<Tuple> scanAllTuples() {
        return new ArrayList<>(tuples);
    }

    public void deleteTuples(List<Tuple> toRemove) {
        tuples.removeAll(toRemove);
    }
}
