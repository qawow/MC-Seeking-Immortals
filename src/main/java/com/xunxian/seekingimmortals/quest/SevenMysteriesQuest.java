package com.xunxian.seekingimmortals.quest;

public final class SevenMysteriesQuest {
    public static final int STAGE_NOT_STARTED = 0;
    public static final int STAGE_ROOT_TEST = 1;
    public static final int STAGE_ENTRY = 2;
    public static final int STAGE_SECRET = 3;
    public static final int STAGE_INFIGHTING = 4;
    public static final int STAGE_LEAVE = 5;
    public static final int STAGE_COMPLETE = 6;

    public static final String NPC_MO_LAO = "墨老先生";
    public static final String NPC_STEWARD = "七玄门执事";

    public static final String FLAG_ROOT_TESTED = "root_tested";
    public static final String FLAG_ADMITTED = "admitted";
    public static final String FLAG_HUANGLONG_MANUAL = "huanglong_manual";
    public static final String FLAG_LABOR_DONE = "labor_done";
    public static final String FLAG_ALCHEMY_LEARNED = "alchemy_learned";
    public static final String FLAG_SECRET_ROOM = "secret_room";
    public static final String FLAG_VIAL_GRANTED = "vial_granted";
    public static final String FLAG_VIAL_USED = "vial_used";
    public static final String FLAG_EVIDENCE = "evidence";
    public static final String FLAG_ATTACK_TRIGGERED = "attack_triggered";
    public static final String FLAG_ESCAPE_READY = "escape_ready";
    public static final String FLAG_YUE_PORTAL = "yue_portal";
    public static final String FLAG_FINAL_REWARD = "final_reward";

    private SevenMysteriesQuest() {}

    public static String stageName(int stage) {
        return switch (stage) {
            case STAGE_ROOT_TEST -> "测灵根";
            case STAGE_ENTRY -> "七玄门入门";
            case STAGE_SECRET -> "发现秘密";
            case STAGE_INFIGHTING -> "宗门内斗";
            case STAGE_LEAVE -> "离开大燕";
            case STAGE_COMPLETE -> "已完成";
            default -> "未开始";
        };
    }

    public static String objective(QuestProgress progress) {
        return switch (progress.getStage()) {
            case STAGE_ROOT_TEST -> "与墨老先生交谈并完成灵根测试";
            case STAGE_ENTRY -> entryObjective(progress);
            case STAGE_SECRET -> secretObjective(progress);
            case STAGE_INFIGHTING -> infightingObjective(progress);
            case STAGE_LEAVE -> leaveObjective(progress);
            case STAGE_COMPLETE -> "七玄门旧事已了，可进入 Phase 10 越国宗门线";
            default -> "与墨老先生交谈开始七玄门任务线";
        };
    }

    private static String entryObjective(QuestProgress progress) {
        if (!progress.hasFlag(FLAG_HUANGLONG_MANUAL)) return "领取黄龙功传承卷轴";
        if (!progress.hasFlag(FLAG_LABOR_DONE)) return "上交 10 份灵草完成劳役";
        if (!progress.hasFlag(FLAG_ALCHEMY_LEARNED)) return "学习基础炼丹术";
        return "等待七玄门执事安排下一步";
    }

    private static String secretObjective(QuestProgress progress) {
        if (!progress.hasFlag(FLAG_SECRET_ROOM)) return "找到并调查墨老密室标记";
        if (!progress.hasFlag(FLAG_VIAL_GRANTED)) return "取得神秘小瓶";
        if (!progress.hasFlag(FLAG_VIAL_USED)) return "用神秘小瓶灵液催熟任意可生长方块";
        return "回报七玄门执事";
    }

    private static String infightingObjective(QuestProgress progress) {
        if (!progress.hasFlag(FLAG_EVIDENCE)) return "取得长老勾结证据";
        if (progress.getBranchChoice().isBlank()) return "选择 report、silent 或 blackmail 处理证据";
        return "宗门内斗已定，准备离开大燕";
    }

    private static String leaveObjective(QuestProgress progress) {
        if (!progress.hasFlag(FLAG_ATTACK_TRIGGERED)) return "等待天罡盟攻打事件";
        if (!progress.hasFlag(FLAG_ESCAPE_READY)) return "逃离七玄门";
        if (!progress.hasFlag(FLAG_YUE_PORTAL)) return "找到越国传送门标记";
        return "穿过越国传送门";
    }
}
