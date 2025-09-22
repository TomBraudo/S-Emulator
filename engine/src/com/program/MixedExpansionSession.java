package com.program;

import com.commands.BaseCommand;
import com.dto.CommandTreeNodeDto;
import com.dto.ProgramTreeDto;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MixedExpansionSession {
    static final class Node {
        final long id;
        final BaseCommand command;
        final List<Node> children = new ArrayList<>();
        Node(long id, BaseCommand command){
            this.id = id;
            this.command = command;
        }
    }

    private final List<Node> roots;
    private final Map<Long, Node> idToNode;
    private final Set<Long> expandedIds = new HashSet<>();
    private final String programName;

    private MixedExpansionSession(String programName, List<Node> roots, Map<Long, Node> idToNode){
        this.programName = programName;
        this.roots = roots;
        this.idToNode = idToNode;
    }

    public static MixedExpansionSession buildFromProgram(Program p){
        long[] idGen = new long[]{1L};

        // Level 0 nodes from program commands
        List<Node> levelNodes = new ArrayList<>();
        Map<Long, Node> idToNode = new HashMap<>();
        for (BaseCommand cmd : p.getCommands()){
            Node n = new Node(idGen[0]++, cmd);
            idToNode.put(n.id, n);
            levelNodes.add(n);
        }
        List<Node> roots = new ArrayList<>(levelNodes);

        int maxLevel = p.getMaxExpansionLevel();
        for (int depth = 0; depth < maxLevel; depth++){
            // Compute counters across the entire level
            int maxLabel = 0;
            int maxWorkVar = 0;
            for (Node n : levelNodes){
                String label = n.command.getLabel();
                if (label != null && label.startsWith("L")){
                    try { maxLabel = Math.max(maxLabel, Integer.parseInt(label.substring(1))); } catch (NumberFormatException ignored) {}
                }
                for (String var : n.command.getPresentVariables()){
                    if (var != null && var.startsWith("z")){
                        try { maxWorkVar = Math.max(maxWorkVar, Integer.parseInt(var.substring(1))); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            AtomicInteger nextLabel = new AtomicInteger(maxLabel + 1);
            AtomicInteger nextWorkVar = new AtomicInteger(maxWorkVar + 1);
            AtomicInteger realIndex = new AtomicInteger(0);

            List<Node> nextLevel = new ArrayList<>();
            for (Node n : levelNodes){
                List<BaseCommand> children = n.command.expand(nextWorkVar, nextLabel, realIndex);
                if (children == null || children.isEmpty()){
                    continue;
                }
                for (BaseCommand c : children){
                    Node child = new Node(idGen[0]++, c);
                    idToNode.put(child.id, child);
                    n.children.add(child);
                    nextLevel.add(child);
                }
            }
            levelNodes = nextLevel;
            if (levelNodes.isEmpty()) break;
        }

        return new MixedExpansionSession(p.getName(), roots, idToNode);
    }

    public ProgramTreeDto toDto(){
        // First assign indices to visible leaves according to expandedIds
        AtomicInteger indexCounter = new AtomicInteger(0);
        for (Node root : roots){
            assignIndices(root, indexCounter);
        }

        List<CommandTreeNodeDto> rootDtos = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++){
            rootDtos.add(toDtoNode(roots.get(i), List.of(i)));
        }
        return new ProgramTreeDto(programName, rootDtos);
    }

    private void assignIndices(Node node, AtomicInteger counter){
        boolean expanded = expandedIds.contains(node.id);
        if (expanded && !node.children.isEmpty()){
            for (Node child : node.children){
                assignIndices(child, counter);
            }
        } else {
            node.command.setIndex(counter.getAndIncrement());
        }
    }

    private CommandTreeNodeDto toDtoNode(Node node, List<Integer> path){
        boolean expanded = expandedIds.contains(node.id);
        List<CommandTreeNodeDto> childrenDtos = List.of();
        if (expanded && !node.children.isEmpty()){
            List<CommandTreeNodeDto> arr = new ArrayList<>();
            for (int i = 0; i < node.children.size(); i++){
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                arr.add(toDtoNode(node.children.get(i), childPath));
            }
            childrenDtos = arr;
        }
        return new CommandTreeNodeDto(
                node.id,
                path,
                node.command.toDisplayString(),
                node.command.getLabel(),
                node.command.isBaseCommand(),
                expanded,
                childrenDtos
        );
    }

    public void expandByPath(List<Integer> path){
        Node n = resolvePath(path);
        if (n != null && !n.children.isEmpty()){
            expandedIds.add(n.id);
        }
    }

    public void collapseByPath(List<Integer> path){
        Node n = resolvePath(path);
        if (n != null){
            expandedIds.remove(n.id);
        }
    }

    private Node resolvePath(List<Integer> path){
        if (path == null || path.isEmpty()) return null;
        int rootIdx = path.get(0);
        if (rootIdx < 0 || rootIdx >= roots.size()) return null;
        Node cur = roots.get(rootIdx);
        for (int i = 1; i < path.size() && cur != null; i++){
            int childIdx = path.get(i);
            if (childIdx < 0 || childIdx >= cur.children.size()) return null;
            cur = cur.children.get(childIdx);
        }
        return cur;
    }
}


