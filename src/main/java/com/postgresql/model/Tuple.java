package com.postgresql.model;

import com.postgresql.catalog.TableMetadata;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Tuple implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Object> values;
    private transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Tuple(List<Object> values) {
        this.values = values;
        this.lock = new ReentrantReadWriteLock();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    }

    public void acquireReadLock() { lock.readLock().lock(); }
    public void releaseReadLock() { lock.readLock().unlock(); }
    public void acquireWriteLock() { lock.writeLock().lock(); }
    public void releaseWriteLock() { lock.writeLock().unlock(); }

    public List<Object> getValues() {
        return values;
    }

    public Object getValue(String columnName, TableMetadata metadata) {
        int index = metadata.getColumnIndex(columnName);
        return values.get(index);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
