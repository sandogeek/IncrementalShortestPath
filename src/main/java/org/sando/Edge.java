package org.sando;

/**
 * 边
 *
 * @author Sando
 * @version 1.0
 * @since 2024/5/2
 */
public class Edge implements IEdge<Integer> {
    private int start;
    private int end;
    private long weight;

    public Edge() {
    }

    public Edge(int start, int end, long weight) {
        this.start = start;
        this.end = end;
        this.weight = weight;
    }

    @Override
    public Integer getStart() {
        return start;
    }

    @Override
    public Integer getEnd() {
        return end;
    }

    @Override
    public long getWeight() {
        return weight;
    }

    @Override
    public void setWeight(long weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "start=" + start +
                ", end=" + end +
                ", weight=" + weight +
                '}';
    }
}
