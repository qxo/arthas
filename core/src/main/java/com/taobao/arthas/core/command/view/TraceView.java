package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MethodNode;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.ThrowNode;
import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceNode;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.Ansi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Term view for TraceModel
 * @author gongdewei 2020/4/29
 */
public class TraceView extends ResultView<TraceModel> {
    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_SKIP_BYPASS_FIRST_CHAR = "+...";
    private static final String STEP_SKIP_BYPASS_END_CHAR = "`...";
    
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";
    private static final String TIME_UNIT = "ms";
    private static final char PERCENTAGE = '%';

    // 是否输出耗时
    private boolean isPrintCost = true;
    private MethodNode maxCostNode;

    public static interface DisplayFilter {
        public boolean notShow(TraceNode node);
    }
    @Override
    public void draw(CommandProcess process, TraceModel result) {
        final String resultFilter = result.getResultFilter();
        DisplayFilter filter = null;
        if (resultFilter != null) {
          if (Pattern.compile("[0-9.]+").matcher(resultFilter).matches()) {
            process.echoTips("displayFilter: hide <= " + resultFilter + " ms\n");
            final double minCost = Double.valueOf(resultFilter) * 1000000;
            filter = new DisplayFilter() {
              public boolean notShow(TraceNode node) {
                if (!(node instanceof MethodNode)) {
                    return false;
                }
                final MethodNode mNode = (MethodNode)node;
                return mNode.getCost() < minCost;
              }
            };
          } else {
            process.echoTips("displayFilter: hide <= " + resultFilter + " by regex find.\n");
            final Pattern hideFilter = Pattern.compile(resultFilter);
             filter = new DisplayFilter() {
               public boolean notShow(TraceNode node) {
                 if (!(node instanceof MethodNode)) {
                     return false;
                 }
                 final MethodNode mNode = (MethodNode)node;
                 return hideFilter.matcher(mNode.getClassName()).find() 
                   || hideFilter.matcher(mNode.getMethodName()).find();
               }
             };
          }
        }
        hideCount = 0;

        final boolean ignoreByPassMethod = result.isIgnoreByPassMethod();
        process.write(drawTree(result.getRoot(), filter, ignoreByPassMethod)).write("\n");  
        result.setMaxCostNode(maxCostNode);
        if (resultFilter != null  || ignoreByPassMethod ) {
            process.echoTips("hideCount: " + hideCount + " resultFilter: " + resultFilter 
                + " ignoreByPassMethod: " + ignoreByPassMethod + "\n");
        }
    }

    private int hideCount = 0;
    public String drawTree(TraceNode root, final DisplayFilter filter, final boolean ignoreByPassMethod) {

        maxCostNode = findMaxCostNode(root, null);

        final StringBuilder treeSB = new StringBuilder(2048);

        final Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);
        final Ansi highlightedChild = Ansi.ansi().fg(Ansi.Color.YELLOW);

        recursive(0, true, "", root, filter, ignoreByPassMethod, true, 0, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, TraceNode node, final int byPassLevel) {
                treeSB.append(prefix);
                if (byPassLevel > 0) {
                    treeSB.append(isLast ? STEP_SKIP_BYPASS_END_CHAR : STEP_SKIP_BYPASS_FIRST_CHAR);
                } else {
                    treeSB.append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR);
                }
                renderNode(treeSB, node, highlighted, highlightedChild);
                if (!StringUtils.isBlank(node.getMark())) {
                    treeSB.append(" [").append(node.getMark()).append(node.marks() > 1 ? "," + node.marks() : "").append("]");
                }
                treeSB.append("\n");
            }

        });

        return treeSB.toString();
    }

    private void renderNode(StringBuilder sb, TraceNode node, Ansi highlighted, final Ansi highlightedChild) {
        //render cost: [0.366865ms]
        if (isPrintCost && node instanceof MethodNode) {
            final MethodNode methodNode = (MethodNode) node;
            final String costStr = renderCost(methodNode);
            if (node == maxCostNode) {
                // the node with max cost will be highlighted
                sb.append(highlighted.a(costStr).reset().toString());
            } else if (maxCostNode != null && maxCostNode.isChildMaxCostNode(methodNode)) {
                sb.append(highlightedChild.a(costStr).reset().toString());
            } else {
                sb.append(costStr);
            }
        }

        //render method name
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            //clazz.getName() + ":" + method.getName() + "()"
            sb.append(methodNode.getClassName()).append(":").append(methodNode.getMethodName()).append("()");
            // #lineNumber
            if (methodNode.getLineNumber()!= -1) {
                sb.append(" #").append(methodNode.getLineNumber());
            }
        } else if (node instanceof ThreadNode) {
            //render thread info
            ThreadNode threadNode = (ThreadNode) node;
            //ts=2020-04-29 10:34:00;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@18b4aac2
            sb.append(format("ts=%s;thread_name=%s;id=%s;is_daemon=%s;priority=%d;TCCL=%s",
                    DateUtils.formatDate(threadNode.getTimestamp()),
                    threadNode.getThreadName(),
                    Long.toHexString(threadNode.getThreadId()),
                    threadNode.isDaemon(),
                    threadNode.getPriority(),
                    threadNode.getClassloader()));

            //trace_id
            if (threadNode.getTraceId() != null) {
                sb.append(";trace_id=").append(threadNode.getTraceId());
            }
            if (threadNode.getRpcId() != null) {
                sb.append(";rpc_id=").append(threadNode.getRpcId());
            }
        } else if (node instanceof ThrowNode) {
            ThrowNode throwNode = (ThrowNode) node;
            sb.append("throw:").append(throwNode.getException())
                    .append(" #").append(throwNode.getLineNumber())
                    .append(" [").append(throwNode.getMessage()).append("]");

        } else {
            throw new UnsupportedOperationException("unknown trace node: " + node.getClass());
        }
    }

    private String renderCost(MethodNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.getTimes() <= 1) {
            if(node.parent() instanceof ThreadNode) {
                sb.append('[').append(nanoToMillis(node.getCost())).append(TIME_UNIT).append("] ");
            }else {
                MethodNode parentNode = (MethodNode) node.parent();
                String percentage = String.format("%.2f", node.getCost()*100.0/parentNode.getTotalCost());
                sb.append('[').append(percentage).append(PERCENTAGE).append(" ").append(nanoToMillis(node.getCost())).append(TIME_UNIT).append(" ").append("] ");

            }
        } else {
            if(node.parent() instanceof ThreadNode) {
                sb.append("[min=").append(nanoToMillis(node.getMinCost())).append(TIME_UNIT).append(",max=")
                        .append(nanoToMillis(node.getMaxCost())).append(TIME_UNIT).append(",total=")
                        .append(nanoToMillis(node.getTotalCost())).append(TIME_UNIT).append(",count=")
                        .append(node.getTimes()).append("] ");
            }else {
                MethodNode parentNode = (MethodNode) node.parent();
                String percentage = String.format("%.2f",node.getTotalCost()*100.0/parentNode.getTotalCost());
                sb.append('[').append(percentage).append(PERCENTAGE).append(" min=").append(nanoToMillis(node.getMinCost())).append(TIME_UNIT).append(",max=")
                        .append(nanoToMillis(node.getMaxCost())).append(TIME_UNIT).append(",total=")
                        .append(nanoToMillis(node.getTotalCost())).append(TIME_UNIT).append(",count=")
                        .append(node.getTimes()).append("] ");
            }

        }
        return sb.toString();
    }

    /**
     * 递归遍历
     */
    private void recursive(int deep, boolean isLast, String prefix, TraceNode node,
        final DisplayFilter filter, final boolean ignoreByPassMethod, final boolean showCurrentNode, 
        final int byPassLevel, Callback callback) {
        if (filter != null && filter.notShow(node)) {
            hideCount++;
            return;
        }
        final List<TraceNode> childrenOrigin = isLeaf(node) ? null : node.getChildren();
        final List<TraceNode> children;
        int throwNodeCount = 0;
        if (ignoreByPassMethod && childrenOrigin != null) {
            children = new ArrayList<TraceNode>();
            for (final TraceNode child : childrenOrigin) {
                if (filter != null && filter.notShow(child)) {
                    continue;
                }
                children.add(child);
                if (child instanceof ThrowNode) {
                    throwNodeCount++;
                }
            }
        } else {
            children = childrenOrigin;
        }
        boolean showNode  = ignoreByPassMethod ? isLeaf(node) : true;
        final boolean byPassOn = ignoreByPassMethod && children != null &&
            (throwNodeCount == 1 ? children.size() == 2 : children.size() == 1);
        if (!showNode) {
            showNode = byPassOn ? showCurrentNode : true;
        } else {
            hideCount ++;
        }
        if (showNode) {
            callback.callback(deep, isLast, prefix, node, byPassLevel);
        }
        if (byPassOn){
            final TraceNode child = children.get(0);
            if (!isLeaf(child) && child instanceof MethodNode){
                //final String mark = child.getMark();
                //child.setMark(mark == null ? ".." : mark + " ..");
                final String currentPrefix = !showNode ? prefix : isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        showNode ? deep + 1 : deep,
                    true,
                        currentPrefix,
                        child,
                        filter,
                        ignoreByPassMethod,
                        false,
                        showNode ? byPassLevel : byPassLevel + 1,
                        callback
                );
                return;
            }
        }
        if (children != null && !children.isEmpty()) {
            final int size = children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        children.get(index),
                        filter,
                        ignoreByPassMethod,
                        byPassOn ? false : true,
                        showNode ? 0 : byPassLevel + 1,
                        callback
                );
            }
        }
    }

    /**
     * 查找耗时最大的节点，便于后续高亮展示
     * @param node
     */
    private MethodNode findMaxCostNode(TraceNode node, MethodNode maxCostNode) {
        if (node instanceof MethodNode && !isRoot(node) && !isRoot(node.parent())) {
            MethodNode aNode = (MethodNode) node;
            if (aNode.isCostNode(maxCostNode)) {
                maxCostNode = aNode;
            }
        }
        if (!isLeaf(node)) {
            final List<TraceNode> children = node.getChildren();
            if (children != null) {
                MethodNode costNode = null;
                for (TraceNode n: children) {
                   costNode = findMaxCostNode(n, costNode);
                }
                node.setMaxCostNode(costNode);
                if (costNode != null && costNode.isCostNode(maxCostNode)) {
                    maxCostNode = costNode;
                }
            }
        }
        return maxCostNode;
    }

    private boolean isRoot(TraceNode node) {
        return node.parent() == null;
    }

    private boolean isLeaf(TraceNode node) {
        List<TraceNode> children = node.getChildren();
        return children == null || children.isEmpty();
    }


    /**
     * convert nano-seconds to milli-seconds
     */
    double nanoToMillis(long nanoSeconds) {
        return nanoSeconds / 1000000.0;
    }

    /**
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, TraceNode node, int byPassLevel);

    }
}
