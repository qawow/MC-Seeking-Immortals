package com.xunxian.seekingimmortals.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandMutationAuthorityTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path COMMAND_SOURCE = JAVA_ROOT.resolve(
            Path.of("command", "SeekingImmortalsCommand.java"));
    private static final Path METHOD_PACKET_SOURCE = JAVA_ROOT.resolve(
            Path.of("network", "MethodActionPacket.java"));
    private static final Path METHOD_SCREEN_SOURCE = JAVA_ROOT.resolve(
            Path.of("client", "MethodTreeScreen.java"));
    private static final Pattern COMMAND_PERMISSION_TWO = Pattern.compile(
            "\\.\\s*requires\\s*\\(\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*->\\s*"
                    + "\\1\\s*\\.\\s*hasPermission\\s*\\(\\s*2\\s*\\)\\s*\\)");
    private static final Pattern PLAYER_PERMISSION_TWO = Pattern.compile(
            "player\\s*\\.\\s*hasPermissions\\s*\\(\\s*2\\s*\\)");

    @Test
    void mutatingCommandNodesRequirePermissionTwo() throws Exception {
        String source = Files.readString(COMMAND_SOURCE);
        String register = methodSource(source, "public static void register(");
        String root = registeredRoot(register);
        List<List<String>> routes = List.of(
                List.of("artifact", "refine"),
                List.of("artifact", "natal", "bind"),
                List.of("artifact", "natal", "grow"),
                List.of("quest", "text", "start"),
                List.of("quest", "text", "advance"),
                List.of("quest", "text", "branch"),
                List.of("quest", "text", "talk"),
                List.of("quest", "text", "gui"),
                List.of("quest", "text", "hooks", "accept"),
                List.of("quest", "text", "interact"),
                List.of("quest", "text", "story", "start"),
                List.of("quest", "text", "story", "complete"),
                List.of("npc", "talk"),
                List.of("npc", "act"),
                List.of("catalog", "manual"),
                List.of("catalog", "methods", "learn"),
                List.of("catalog", "refine", "craft"),
                List.of("catalog", "formations", "deploy"),
                List.of("catalog", "talisman", "craft"),
                List.of("catalog", "puppet", "craft"),
                List.of("catalog", "chronicle", "discover"),
                List.of("catalog", "beast", "contract"),
                List.of("boss"),
                List.of("phase", "mark"),
                List.of("phase", "enter"));

        for (List<String> route : routes) {
            String node = commandNode(root, route);
            assertPermissionTwo(node, String.join(" ", route));
        }

        String mission = commandNode(root, List.of("mission"));
        assertPermissionTwo(mission, "mission / mission gen");
        String missionGen = directLiteralChild(mission, "gen");
        assertTrue(codeOnly(directBuilderChain(mission)).contains("missionGenerate"),
                "mission root must not remain an unguarded generation alias");
        assertTrue(codeOnly(directBuilderChain(missionGen)).contains("missionGenerate"),
                "mission gen must remain under the permission-gated mission node");
    }

    @Test
    void methodPacketRestrictsOnlyLearnActionToPermissionTwo() throws Exception {
        String source = Files.readString(METHOD_PACKET_SOURCE);
        String handle = methodSource(source, "public static void handle(");
        String learnBranch = startsWithBranch(handle, "learn:");
        String cultivateBranch = startsWithBranch(handle, "cultivate:");

        assertTrue(permissionGuardsMutation(
                        learnBranch, Pattern.compile("ManualCatalogService\\s*\\.\\s*learnMethod\\s*\\(")),
                "learn action must fail closed unless player.hasPermissions(2)");
        assertFalse(PLAYER_PERMISSION_TWO.matcher(codeOnly(cultivateBranch)).find(),
                "cultivate action must not inherit the operator-only learn restriction");
    }

    @Test
    void methodTreeHidesAndDisablesLearnForNormalPlayers() throws Exception {
        String source = Files.readString(METHOD_SCREEN_SOURCE);
        String update = codeOnly(methodSource(source, "private void updateLearnButton("));
        Matcher permissionLocal = Pattern.compile(
                "\\bboolean\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*"
                        + "([^;]*?player\\s*\\.\\s*hasPermissions\\s*\\(\\s*2\\s*\\)[^;]*);")
                .matcher(update);
        assertTrue(permissionLocal.find(),
                "updateLearnButton must derive an operator-only Learn permission predicate");

        String permissionName = permissionLocal.group(1);
        String permissionExpression = compact(permissionLocal.group(2));
        assertFalse(Pattern.compile("!\\s*(?:[A-Za-z_$][A-Za-z0-9_$]*\\s*\\.\\s*)*"
                        + "hasPermissions\\s*\\(\\s*2\\s*\\)")
                        .matcher(permissionExpression).find(),
                "the Learn permission predicate must be positive for operators");

        String visibleExpression = assignmentExpression(update, "visible");
        String activeExpression = assignmentExpression(update, "active");
        assertPositiveReference(visibleExpression, permissionName,
                "normal players must not see the Learn button");
        assertPositiveReference(activeExpression, permissionName,
                "normal players must not activate the Learn button");
    }

    private static void assertPermissionTwo(String node, String route) {
        String ownChain = codeOnly(directBuilderChain(node));
        assertTrue(COMMAND_PERMISSION_TWO.matcher(ownChain).find(),
                route + " must declare requires(source -> source.hasPermission(2)) on its own node");
    }

    private static String commandNode(String root, List<String> route) {
        String node = root;
        for (String segment : route) {
            node = directLiteralChild(node, segment);
        }
        return node;
    }

    private static String registeredRoot(String registerMethod) {
        String code = codeOnly(registerMethod);
        int call = code.indexOf("dispatcher.register");
        assertTrue(call >= 0, "register method must register the root command");
        int opening = code.indexOf('(', call);
        assertTrue(opening >= 0, "dispatcher.register must have an argument");
        int closing = matchingDelimiter(code, opening, '(', ')');
        int argumentStart = skipWhitespace(code, opening + 1);
        String root = registerMethod.substring(argumentStart, closing);
        assertTrue(literalAt(root, 0, "seeking_immortals"),
                "registered root must be seeking_immortals");
        return root;
    }

    private static String directLiteralChild(String parent, String name) {
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
            if (literalAt(parent, argumentStart, name)) {
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

    private static String startsWithBranch(String method, String actionPrefix) {
        Pattern condition = Pattern.compile(
                "\\b(?:if|else\\s+if)\\s*\\(\\s*lower\\s*\\.\\s*startsWith\\s*"
                        + "\\(\\s*\"" + Pattern.quote(actionPrefix) + "\"\\s*\\)\\s*\\)");
        Matcher matcher = condition.matcher(method);
        assertTrue(matcher.find(), "missing packet action branch: " + actionPrefix);
        String code = codeOnly(method);
        int opening = code.indexOf('{', matcher.end());
        assertTrue(opening >= 0, "packet action branch must use a block: " + actionPrefix);
        int closing = matchingDelimiter(code, opening, '{', '}');
        return method.substring(opening + 1, closing);
    }

    private static boolean permissionGuardsMutation(String branch, Pattern mutationPattern) {
        String code = codeOnly(branch);
        Matcher mutationMatcher = mutationPattern.matcher(code);
        assertTrue(mutationMatcher.find(), "missing guarded mutation in packet branch");
        int mutation = mutationMatcher.start();

        for (int cursor = 0; cursor < mutation;) {
            int conditional = nextWord(code, "if", cursor);
            if (conditional < 0 || conditional >= mutation) {
                break;
            }
            int conditionStart = skipWhitespace(code, conditional + 2);
            if (conditionStart >= code.length() || code.charAt(conditionStart) != '(') {
                cursor = conditional + 2;
                continue;
            }
            int conditionEnd = matchingDelimiter(code, conditionStart, '(', ')');
            String condition = compact(code.substring(conditionStart + 1, conditionEnd));
            int bodyStart = skipWhitespace(code, conditionEnd + 1);
            if (bodyStart >= code.length() || code.charAt(bodyStart) != '{') {
                cursor = conditionEnd + 1;
                continue;
            }
            int bodyEnd = matchingDelimiter(code, bodyStart, '{', '}');
            boolean negativePermission = condition.contains("!player.hasPermissions(2)");
            boolean positivePermission = condition.contains("player.hasPermissions(2)")
                    && !negativePermission;
            if (positivePermission && mutation > bodyStart && mutation < bodyEnd) {
                return true;
            }
            if (negativePermission && bodyEnd < mutation
                    && compact(code.substring(bodyStart + 1, bodyEnd)).contains("return;")) {
                return true;
            }
            cursor = conditionEnd + 1;
        }
        return false;
    }

    private static String assignmentExpression(String method, String property) {
        Pattern assignment = Pattern.compile(
                "learnButton\\s*\\.\\s*" + Pattern.quote(property) + "\\s*=\\s*([^;]+);");
        Matcher matcher = assignment.matcher(method);
        assertTrue(matcher.find(), "updateLearnButton must assign learnButton." + property);
        return matcher.group(1);
    }

    private static void assertPositiveReference(String expression, String permissionName, String message) {
        String compact = compact(expression);
        Pattern reference = Pattern.compile(
                "(?<![!A-Za-z0-9_$])" + Pattern.quote(permissionName) + "(?![A-Za-z0-9_$])");
        assertTrue(reference.matcher(compact).find(), message);
        assertFalse(Pattern.compile("!" + Pattern.quote(permissionName) + "(?![A-Za-z0-9_$])")
                .matcher(compact).find(), message);
    }

    private static int nextWord(String source, String word, int from) {
        int cursor = from;
        while ((cursor = source.indexOf(word, cursor)) >= 0) {
            boolean leftBoundary = cursor == 0 || !Character.isJavaIdentifierPart(source.charAt(cursor - 1));
            int end = cursor + word.length();
            boolean rightBoundary = end == source.length()
                    || !Character.isJavaIdentifierPart(source.charAt(end));
            if (leftBoundary && rightBoundary) {
                return cursor;
            }
            cursor = end;
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

    private static int matchingDelimiter(String code, int opening, char open, char close) {
        assertTrue(opening >= 0 && opening < code.length() && code.charAt(opening) == open,
                "missing opening delimiter " + open);
        int depth = 0;
        for (int index = opening; index < code.length(); index++) {
            char current = code.charAt(index);
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

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
