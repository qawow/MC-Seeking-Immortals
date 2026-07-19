package com.xunxian.seekingimmortals.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuCommandPermissionTest {
    private static final Path COMMAND_SOURCE = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals",
            "command", "SeekingImmortalsCommand.java");
    private static final Pattern PERMISSION_TWO = Pattern.compile(
            "\\.\\s*requires\\s*\\(\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*->\\s*"
                    + "\\1\\s*\\.\\s*hasPermission\\s*\\(\\s*2\\s*\\)\\s*\\)");

    @Test
    void directMenuBusinessCommandsRequirePermissionTwo() throws Exception {
        String root = registeredRoot(Files.readString(COMMAND_SOURCE));
        List<MutationRoute> routes = List.of(
                new MutationRoute(List.of("market", "open"), "marketOpen"),
                new MutationRoute(List.of("market", "buy"), "marketBuy"),
                new MutationRoute(List.of("catalog", "auction", "open"), "catalogAuctionOpen"),
                new MutationRoute(List.of("catalog", "auction", "interest"), "catalogAuctionInterest"),
                new MutationRoute(List.of("catalog", "auction", "bid"), "catalogAuctionBid"),
                new MutationRoute(List.of("catalog", "auction", "settle"), "catalogAuctionSettle"),
                new MutationRoute(List.of("sect", "open"), "sectOpen"),
                new MutationRoute(List.of("sect", "join"), "sectJoin"),
                new MutationRoute(List.of("sect", "apply"), "sectApply"),
                new MutationRoute(List.of("sect", "advance"), "sectAdvance"),
                new MutationRoute(List.of("sect", "buy"), "sectBuy"),
                new MutationRoute(List.of("sect", "donate"), "sectDonateSpiritGrass"));

        for (MutationRoute route : routes) {
            String node = commandNode(root, route.segments());
            String display = String.join(" ", route.segments());
            assertTrue(PERMISSION_TWO.matcher(codeOnly(directBuilderChain(node))).find(),
                    display + " must declare permission 2 on its own command node");
            assertTrue(codeOnly(node).contains(route.handler() + "("),
                    display + " must still route to " + route.handler());
        }
    }

    @Test
    void readOnlyCommandsStayOpenAndMarketRootIsSafe() throws Exception {
        String root = registeredRoot(Files.readString(COMMAND_SOURCE));
        List<List<String>> openRoutes = List.of(
                List.of("market"),
                List.of("market", "list"),
                List.of("catalog"),
                List.of("catalog", "auction"),
                List.of("catalog", "auction", "list"),
                List.of("sect"),
                List.of("sect", "status"),
                List.of("sect", "candidates"),
                List.of("sect", "shop"));

        for (List<String> route : openRoutes) {
            String node = commandNode(root, route);
            assertFalse(PERMISSION_TWO.matcher(codeOnly(directBuilderChain(node))).find(),
                    String.join(" ", route) + " must remain available to normal players");
        }

        String marketRoot = codeOnly(directBuilderChain(commandNode(root, List.of("market"))));
        assertTrue(marketRoot.contains("marketList("),
                "the open market root alias must fall back to a read-only listing");
        assertFalse(marketRoot.contains("marketOpen("),
                "the market root must not retain an unguarded menu-opening alias");

        String auction = commandNode(root, List.of("catalog", "auction"));
        String preview = directArgumentChild(auction, "id");
        assertFalse(PERMISSION_TWO.matcher(codeOnly(directBuilderChain(preview))).find(),
                "catalog auction <id> preview must remain available to normal players");
        assertTrue(codeOnly(preview).contains("catalogAuctionPreview("),
                "catalog auction <id> must remain a read-only preview route");
    }

    private static String registeredRoot(String source) {
        String register = methodSource(source, "public static void register(");
        String code = codeOnly(register);
        int call = code.indexOf("dispatcher.register");
        assertTrue(call >= 0, "register method must register the root command");
        int opening = code.indexOf('(', call);
        assertTrue(opening >= 0, "dispatcher.register must have an argument");
        int closing = matchingDelimiter(code, opening, '(', ')');
        int argumentStart = skipWhitespace(code, opening + 1);
        String root = register.substring(argumentStart, closing);
        assertTrue(literalAt(root, 0, "seeking_immortals"),
                "registered root must be seeking_immortals");
        return root;
    }

    private static String commandNode(String root, List<String> route) {
        String node = root;
        for (String segment : route) {
            node = directLiteralChild(node, segment);
        }
        return node;
    }

    private static String directLiteralChild(String parent, String name) {
        return directChild(parent, name, true);
    }

    private static String directArgumentChild(String parent, String name) {
        return directChild(parent, name, false);
    }

    private static String directChild(String parent, String name, boolean literal) {
        String code = codeOnly(parent);
        int cursor = 0;
        while (true) {
            int then = firstDirectThen(code, cursor);
            if (then < 0) {
                throw new AssertionError("missing direct command child: " + name);
            }
            int opening = skipWhitespace(code, then + ".then".length());
            assertTrue(opening < code.length() && code.charAt(opening) == '(',
                    "malformed direct child for: " + name);
            int closing = matchingDelimiter(code, opening, '(', ')');
            int argumentStart = skipWhitespace(code, opening + 1);
            boolean matches = literal
                    ? literalAt(parent, argumentStart, name)
                    : argumentAt(parent, argumentStart, name);
            if (matches) {
                return parent.substring(argumentStart, closing);
            }
            cursor = closing + 1;
        }
    }

    private static boolean literalAt(String source, int start, String name) {
        Pattern literal = Pattern.compile(
                "Commands\\s*\\.\\s*literal\\s*\\(\\s*\\Q\"" + name + "\"\\E\\s*\\)");
        Matcher matcher = literal.matcher(source);
        matcher.region(start, source.length());
        return matcher.lookingAt();
    }

    private static boolean argumentAt(String source, int start, String name) {
        Pattern argument = Pattern.compile(
                "Commands\\s*\\.\\s*argument\\s*\\(\\s*\\Q\"" + name + "\"\\E\\s*,");
        Matcher matcher = argument.matcher(source);
        matcher.region(start, source.length());
        return matcher.lookingAt();
    }

    private static String directBuilderChain(String node) {
        int firstChild = firstDirectThen(codeOnly(node), 0);
        return firstChild < 0 ? node : node.substring(0, firstChild);
    }

    private static int firstDirectThen(String code, int from) {
        int depth = 0;
        for (int index = from; index < code.length(); index++) {
            char current = code.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                assertTrue(depth >= 0, "unbalanced command expression");
            } else if (depth == 0 && code.startsWith(".then", index)) {
                return index;
            }
        }
        return -1;
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        String code = codeOnly(source);
        int opening = code.indexOf('{', start);
        assertTrue(opening >= 0, "missing method body: " + declaration);
        int closing = matchingDelimiter(code, opening, '{', '}');
        return source.substring(start, closing + 1);
    }

    private static int matchingDelimiter(String source, int opening, char open, char close) {
        assertTrue(opening >= 0 && opening < source.length() && source.charAt(opening) == open,
                "missing opening delimiter " + open);
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == open) {
                depth++;
            } else if (current == close && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("unterminated delimiter " + open + close);
    }

    private static int skipWhitespace(String source, int start) {
        int cursor = start;
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static String codeOnly(String source) {
        char[] code = source.toCharArray();
        for (int index = 0; index < source.length();) {
            char current = source.charAt(index);
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                int end = source.indexOf('\n', index + 2);
                end = end < 0 ? source.length() : end;
                mask(code, index, end);
                index = end;
            } else if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                int end = source.indexOf("*/", index + 2);
                end = end < 0 ? source.length() : end + 2;
                mask(code, index, end);
                index = end;
            } else if (current == '"' || current == '\'') {
                int end = quotedEnd(source, index, current);
                mask(code, index, end);
                index = end;
            } else {
                index++;
            }
        }
        return new String(code);
    }

    private static int quotedEnd(String source, int opening, char quote) {
        for (int index = opening + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '\\') {
                index++;
            } else if (current == quote) {
                return index + 1;
            }
        }
        return source.length();
    }

    private static void mask(char[] source, int start, int end) {
        for (int index = start; index < end; index++) {
            if (source[index] != '\n' && source[index] != '\r') {
                source[index] = ' ';
            }
        }
    }

    private record MutationRoute(List<String> segments, String handler) {}
}
