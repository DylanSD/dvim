package com.dksd.dvim.ascii;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ASCII Architecture Diagram Generator
 * Creates ASCII diagrams with nodes, actions, and connections
 */
public class AsciiDiagramGenerator {

    public enum ArrowType {
        DOWN("v", "|", "v"),
        UP("^", "|", "^"),
        RIGHT(">", "-", ">"),
        LEFT("<", "-", "<"),
        BIDIRECTIONAL("<", "-", ">"),
        DOWN_ARROW("", "|", "v"),
        UP_ARROW("^", "|", ""),
        RIGHT_ARROW("", "-", ">"),
        LEFT_ARROW("<", "-", "");

        private final String start;
        private final String line;
        private final String end;

        ArrowType(String start, String line, String end) {
            this.start = start;
            this.line = line;
            this.end = end;
        }
    }

    public enum BoxStyle {
        SOLID('+', '-', '|'),
        DOUBLE('#', '=', '#'),
        ROUNDED('.', '-', '|'),
        DASHED('+', '.', ':'),
        HEAVY('█', '▄', '█');

        private final char corner;
        private final char horizontal;
        private final char vertical;

        BoxStyle(char corner, char horizontal, char vertical) {
            this.corner = corner;
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }

    /**
     * Represents a node in the diagram
     */
    public static class Node {
        private final String id;
        private final List<String> content;
        private final BoxStyle style;
        private int width;
        private int height;
        private int x;
        private int y;

        public Node(String id, String content) {
            this(id, content, BoxStyle.SOLID);
        }

        public Node(String id, String content, BoxStyle style) {
            this.id = id;
            this.content = Arrays.asList(content.split("\n"));
            this.style = style;
            calculateDimensions();
        }

        public Node(String id, List<String> content, BoxStyle style) {
            this.id = id;
            this.content = content;
            this.style = style;
            calculateDimensions();
        }

        private void calculateDimensions() {
            this.width = content.stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(10) + 4; // Add padding
            this.height = content.size() + 2; // Add top and bottom borders
        }

        public String getId() { return id; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getX() { return x; }
        public int getY() { return y; }
        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public List<String> render() {
            List<String> lines = new ArrayList<>();

            // Top border
            lines.add(renderBorder(true));

            // Content lines with side borders
            for (String line : content) {
                lines.add(renderContentLine(line));
            }

            // Bottom border
            lines.add(renderBorder(false));

            return lines;
        }

        private String renderBorder(boolean isTop) {
            StringBuilder border = new StringBuilder();
            border.append(style.corner);
            for (int i = 0; i < width - 2; i++) {
                border.append(style.horizontal);
            }
            border.append(style.corner);
            return border.toString();
        }

        private String renderContentLine(String content) {
            StringBuilder line = new StringBuilder();
            line.append(style.vertical);
            line.append(" ");
            line.append(content);

            // Pad to width
            int padding = width - content.length() - 3;
            for (int i = 0; i < padding; i++) {
                line.append(" ");
            }
            line.append(style.vertical);
            return line.toString();
        }
    }

    /**
     * Represents an action/connection between nodes
     */
    public static class Action {
        private final String fromNodeId;
        private final String toNodeId;
        private final String label;
        private final ArrowType arrowType;

        public Action(String fromNodeId, String toNodeId) {
            this(fromNodeId, toNodeId, "", ArrowType.DOWN);
        }

        public Action(String fromNodeId, String toNodeId, String label) {
            this(fromNodeId, toNodeId, label, ArrowType.DOWN);
        }

        public Action(String fromNodeId, String toNodeId, String label, ArrowType arrowType) {
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.label = label;
            this.arrowType = arrowType;
        }

        public String getFromNodeId() { return fromNodeId; }
        public String getToNodeId() { return toNodeId; }
        public String getLabel() { return label; }
        public ArrowType getArrowType() { return arrowType; }
    }

    /**
     * Diagram builder using fluent API
     */
    public static class DiagramBuilder {
        private final List<Node> nodes = new ArrayList<>();
        private final List<Action> actions = new ArrayList<>();
        private String title;
        private int spacing = 2;

        public DiagramBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public DiagramBuilder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public DiagramBuilder addNode(String id, String content) {
            nodes.add(new Node(id, content));
            return this;
        }

        public DiagramBuilder addNode(String id, String content, BoxStyle style) {
            nodes.add(new Node(id, content, style));
            return this;
        }

        public DiagramBuilder addNode(Node node) {
            nodes.add(node);
            return this;
        }

        public DiagramBuilder addAction(String fromNodeId, String toNodeId) {
            actions.add(new Action(fromNodeId, toNodeId));
            return this;
        }

        public DiagramBuilder addAction(String fromNodeId, String toNodeId, String label) {
            actions.add(new Action(fromNodeId, toNodeId, label));
            return this;
        }

        public DiagramBuilder addAction(Action action) {
            actions.add(action);
            return this;
        }

        public Diagram build() {
            return new Diagram(title, nodes, actions, spacing);
        }
    }

    /**
     * The complete diagram
     */
    public static class Diagram {
        private final String title;
        private final List<Node> nodes;
        private final List<Action> actions;
        private final int spacing;
        private char[][] canvas;
        private int canvasWidth;
        private int canvasHeight;

        public Diagram(String title, List<Node> nodes, List<Action> actions, int spacing) {
            this.title = title;
            this.nodes = nodes;
            this.actions = actions;
            this.spacing = spacing;
            layoutNodes();
            initializeCanvas();
        }

        private void layoutNodes() {
            // Simple vertical layout
            int currentY = (title != null) ? 3 : 1;
            int maxWidth = 0;

            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                node.setPosition(2, currentY);
                maxWidth = Math.max(maxWidth, node.getWidth());
                currentY += node.getHeight() + spacing;
            }

            // Center align nodes
            for (Node node : nodes) {
                int centerOffset = (maxWidth - node.getWidth()) / 2;
                node.setPosition(node.getX() + centerOffset, node.getY());
            }
        }

        private void initializeCanvas() {
            // Calculate canvas dimensions
            canvasWidth = nodes.stream()
                    .mapToInt(n -> n.getX() + n.getWidth())
                    .max()
                    .orElse(50) + 5;

            canvasHeight = nodes.stream()
                    .mapToInt(n -> n.getY() + n.getHeight())
                    .max()
                    .orElse(20) + 3;

            // Initialize with spaces
            canvas = new char[canvasHeight][canvasWidth];
            for (char[] row : canvas) {
                Arrays.fill(row, ' ');
            }
        }

        public String render() {
            // Draw title if present
            if (title != null) {
                drawTitle();
            }

            // Draw nodes
            for (Node node : nodes) {
                drawNode(node);
            }

            // Draw connections
            for (Action action : actions) {
                drawAction(action);
            }

            // Convert canvas to string
            return canvasToString();
        }

        private void drawTitle() {
            int centerX = (canvasWidth - title.length()) / 2;
            drawText(centerX, 0, title);

            // Underline
            String underline = "=".repeat(title.length());
            drawText(centerX, 1, underline);
        }

        private void drawNode(Node node) {
            List<String> nodeLines = node.render();
            for (int i = 0; i < nodeLines.size(); i++) {
                drawText(node.getX(), node.getY() + i, nodeLines.get(i));
            }
        }

        private void drawAction(Action action) {
            Node fromNode = findNode(action.getFromNodeId());
            Node toNode = findNode(action.getToNodeId());

            if (fromNode == null || toNode == null) return;

            // Calculate connection points
            int fromX = fromNode.getX() + fromNode.getWidth() / 2;
            int fromY = fromNode.getY() + fromNode.getHeight();
            int toX = toNode.getX() + toNode.getWidth() / 2;
            int toY = toNode.getY() - 1;

            // Draw vertical arrow
            drawArrow(fromX, fromY, toX, toY, action.getArrowType());

            // Draw label if present
            if (action.getLabel() != null && !action.getLabel().isEmpty()) {
                int labelX = fromX - action.getLabel().length() / 2;
                int labelY = (fromY + toY) / 2;
                drawText(labelX, labelY, action.getLabel());
            }
        }

        private void drawArrow(int fromX, int fromY, int toX, int toY, ArrowType type) {
            if (fromY < toY) {
                // Draw vertical line down
                for (int y = fromY; y < toY; y++) {
                    if (canvas[y][fromX] == ' ') {
                        canvas[y][fromX] = '|';
                    }
                }
                // Add arrow head
                if (toY - 1 >= 0 && toY - 1 < canvasHeight && fromX < canvasWidth) {
                    canvas[toY - 1][fromX] = 'v';
                }
            } else if (fromX < toX) {
                // Draw horizontal line right
                for (int x = fromX; x < toX; x++) {
                    if (canvas[fromY][x] == ' ') {
                        canvas[fromY][x] = '-';
                    }
                }
                canvas[fromY][toX - 1] = '>';
            }
        }

        private void drawText(int x, int y, String text) {
            if (y < 0 || y >= canvasHeight) return;

            for (int i = 0; i < text.length(); i++) {
                if (x + i < canvasWidth) {
                    canvas[y][x + i] = text.charAt(i);
                }
            }
        }

        private Node findNode(String nodeId) {
            return nodes.stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst()
                    .orElse(null);
        }

        private String canvasToString() {
            StringBuilder result = new StringBuilder();
            for (char[] row : canvas) {
                // Trim trailing spaces from each row
                String line = new String(row).replaceAll("\\s+$", "");
                result.append(line).append("\n");
            }
            return result.toString();
        }
    }

    /**
     * Example usage and test cases
     */
    public static void main(String[] args) {
        // Example 1: Load Testing Architecture
        System.out.println("=== Load Testing Architecture ===\n");

        Diagram loadTestDiagram = new DiagramBuilder()
                .withTitle("Load Testing Architecture")
                .addNode("load", "Load Generator", BoxStyle.SOLID)
                .addNode("traffic", "500 → 1000 RPS\ntraffic", BoxStyle.SOLID)
                .addNode("service", "HTTP Service\n(Go/Java/Py/JS/R)", BoxStyle.SOLID)
                .addNode("disk", "Disk Writes", BoxStyle.SOLID)
                .addAction("load", "traffic")
                .addAction("traffic", "service")
                .addAction("service", "disk")
                .build();

        System.out.println(loadTestDiagram.render());

        // Example 2: Microservices Architecture
        System.out.println("\n=== Microservices Architecture ===\n");

        Diagram microservicesDiagram = new DiagramBuilder()
                .withTitle("Microservices Flow")
                .addNode("client", "Web Client", BoxStyle.DOUBLE)
                .addNode("gateway", "API Gateway\n(Auth/Routing)", BoxStyle.SOLID)
                .addNode("users", "User Service\nPostgreSQL", BoxStyle.SOLID)
                .addNode("orders", "Order Service\nMongoDB", BoxStyle.SOLID)
                .addNode("queue", "Message Queue\n(RabbitMQ)", BoxStyle.DASHED)
                .addAction("client", "gateway", "HTTPS")
                .addAction("gateway", "users", "REST")
                .addAction("gateway", "orders", "REST")
                .addAction("orders", "queue", "Events")
                .build();

        System.out.println(microservicesDiagram.render());

        // Example 3: CI/CD Pipeline
        System.out.println("\n=== CI/CD Pipeline ===\n");

        Diagram cicdDiagram = new DiagramBuilder()
                .withTitle("CI/CD Pipeline")
                .withSpacing(1)
                .addNode("commit", "Git Commit", BoxStyle.ROUNDED)
                .addNode("build", "Build & Test\n(Jenkins)", BoxStyle.SOLID)
                .addNode("scan", "Security Scan\n(SonarQube)", BoxStyle.SOLID)
                .addNode("deploy", "Deploy to K8s", BoxStyle.SOLID)
                .addNode("monitor", "Monitoring\n(Prometheus)", BoxStyle.HEAVY)
                .addAction("commit", "build", "webhook")
                .addAction("build", "scan")
                .addAction("scan", "deploy", "approved")
                .addAction("deploy", "monitor")
                .build();

        System.out.println(cicdDiagram.render());

        // Example 4: Data Processing Pipeline
        System.out.println("\n=== Data Processing Pipeline ===\n");

        List<String> etlContent = Arrays.asList(
                "ETL Pipeline",
                "• Extract",
                "• Transform",
                "• Load"
        );

        Node etlNode = new Node("etl", etlContent, BoxStyle.DOUBLE);

        Diagram dataDiagram = new DiagramBuilder()
                .addNode("source", "Data Sources\nAPIs/Files/DBs", BoxStyle.SOLID)
                .addNode(etlNode)
                .addNode("warehouse", "Data Warehouse\n(Snowflake)", BoxStyle.SOLID)
                .addNode("analytics", "Analytics\nDashboards", BoxStyle.DASHED)
                .addAction("source", "etl", "batch")
                .addAction("etl", "warehouse", "load")
                .addAction("warehouse", "analytics", "query")
                .build();

        System.out.println(dataDiagram.render());

        // Example 5: Event-Driven Architecture
        System.out.println("\n=== Event-Driven Architecture ===\n");

        Diagram eventDiagram = new DiagramBuilder()
                .withTitle("Event Processing")
                .addNode("producer", "Event Producer", BoxStyle.SOLID)
                .addNode("broker", "Kafka Broker\nPartitioned Topics", BoxStyle.DOUBLE)
                .addNode("consumer1", "Consumer A\n(Payments)", BoxStyle.SOLID)
                .addNode("consumer2", "Consumer B\n(Analytics)", BoxStyle.SOLID)
                .addNode("storage", "Event Store", BoxStyle.HEAVY)
                .addAction("producer", "broker", "publish")
                .addAction("broker", "consumer1", "subscribe")
                .addAction("broker", "consumer2", "subscribe")
                .addAction("broker", "storage", "persist")
                .build();

        System.out.println(eventDiagram.render());
    }

    /**
     * Utility method to create simple diagrams quickly
     */
    public static String quickDiagram(String... nodes) {
        DiagramBuilder builder = new DiagramBuilder();

        for (int i = 0; i < nodes.length; i++) {
            String nodeId = "node" + i;
            builder.addNode(nodeId, nodes[i]);

            if (i > 0) {
                builder.addAction("node" + (i - 1), nodeId);
            }
        }

        return builder.build().render();
    }

    /**
     * Create a diagram from a simple DSL string
     */
    public static String fromDSL(String dsl) {
        // DSL Format: "NodeA -> NodeB -> NodeC"
        // or "NodeA(Label) -> NodeB(Label) -> NodeC"

        DiagramBuilder builder = new DiagramBuilder();
        String[] parts = dsl.split("->");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            String nodeId = "node" + i;
            String label = part;

            // Check for label in parentheses
            if (part.contains("(") && part.contains(")")) {
                label = part.substring(0, part.indexOf("(")).trim();
                String details = part.substring(part.indexOf("(") + 1, part.indexOf(")"));
                label = label + "\n" + details;
            }

            builder.addNode(nodeId, label);

            if (i > 0) {
                builder.addAction("node" + (i - 1), nodeId);
            }
        }

        return builder.build().render();
    }
}