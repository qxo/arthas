package com.taobao.arthas.core.command.model;

/**
 * Data model of TraceCommand
 * @author gongdewei 2020/4/29
 */
public class TraceModel extends ResultModel {
    private TraceNode root;
    private int nodeCount;

    private MethodNode maxCostNode;

    private String resultFilter;

    private boolean ignoreByPassMethod;

    public TraceModel() {
    }

    public TraceModel(TraceNode root, int nodeCount) {
        this.root = root;
        this.nodeCount = nodeCount;
    }

    @Override
    public String getType() {
        return "trace";
    }

    public TraceNode getRoot() {
        return root;
    }

    public void setRoot(TraceNode root) {
        this.root = root;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public MethodNode getMaxCostNode (){
        return maxCostNode;
    }
 
    public void setMaxCostNode(final MethodNode node) {
        maxCostNode = node;
    }

    /**
     * @return String return the displayFilter
     */
    public String getResultFilter() {
        return resultFilter;
    }

    /**
     * @param displayFilter the displayFilter to set
     */
    public void setResultFilter(String displayFilter) {
        this.resultFilter = displayFilter;
    }


    /**
     * @return boolean return the ignoreByPassMethod
     */
    public boolean isIgnoreByPassMethod() {
        return ignoreByPassMethod;
    }

    /**
     * @param ignoreByPassMethod the ignoreByPassMethod to set
     */
    public void setIgnoreByPassMethod(boolean ignoreByPassMethod) {
        this.ignoreByPassMethod = ignoreByPassMethod;
    }

}
