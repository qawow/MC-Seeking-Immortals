package com.xunxian.seekingimmortals.cultivation;

/**
 * 境界属性基准表
 * <p>存储每个境界的灵力基准(manaBase)、神识基准(divSenseBase)、生命基准(hpBase)</p>
 * <p>Phase 1 MVP 实现</p>
 */
public final class RealmStageConfig {
    private RealmStageConfig() {}

    /**
     * 获取指定境界的灵力基准值
     * @param realm 境界
     * @return 灵力基准值（未乘阶段倍率）
     */
    public static int getManaBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 100;
            case FOUNDATION_ESTABLISHMENT -> 250;
            case CORE_FORMATION -> 600;
            case NASCENT_SOUL -> 1200;
            case SOUL_TRANSFORMATION -> 2500;
            case VOID_REFINEMENT -> 5000;
            case UNITY -> 10000;
            case MAHAYANA -> 20000;
            case TRIBULATION -> 40000;
            case TRUE_IMMORTAL -> 80000;
        };
    }

    /**
     * 获取指定境界的神识基准值
     * @param realm 境界
     * @return 神识基准值
     */
    public static int getDivSenseBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 50;
            case FOUNDATION_ESTABLISHMENT -> 150;
            case CORE_FORMATION -> 400;
            case NASCENT_SOUL -> 1000;
            case SOUL_TRANSFORMATION -> 2500;
            case VOID_REFINEMENT -> 6000;
            case UNITY -> 15000;
            case MAHAYANA -> 40000;
            case TRIBULATION -> 100000;
            case TRUE_IMMORTAL -> 250000;
        };
    }

    /**
     * 获取指定境界的生命基准值
     * @param realm 境界
     * @return 生命基准值（HP）
     */
    public static int getHpBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 20;
            case FOUNDATION_ESTABLISHMENT -> 40;
            case CORE_FORMATION -> 80;
            case NASCENT_SOUL -> 160;
            case SOUL_TRANSFORMATION -> 320;
            case VOID_REFINEMENT -> 640;
            case UNITY -> 1280;
            case MAHAYANA -> 2560;
            case TRIBULATION -> 5120;
            case TRUE_IMMORTAL -> 10240;
        };
    }

    /**
     * 获取灵力回速基准（点/秒）
     * @param realm 境界
     * @return 灵力回速基准
     */
    public static float getManaRecoveryBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 0.5f;
            case FOUNDATION_ESTABLISHMENT -> 1.5f;
            case CORE_FORMATION -> 4.0f;
            case NASCENT_SOUL -> 10.0f;
            case SOUL_TRANSFORMATION -> 25.0f;
            case VOID_REFINEMENT -> 60.0f;
            case UNITY -> 150.0f;
            case MAHAYANA -> 400.0f;
            case TRIBULATION -> 1000.0f;
            case TRUE_IMMORTAL -> 2500.0f;
        };
    }

    /**
     * 获取修为回速基准（点/秒）
     * @param realm 境界
     * @return 修为回速基准
     */
    public static float getCultivationGainBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 0.1f;
            case FOUNDATION_ESTABLISHMENT -> 0.08f;
            case CORE_FORMATION -> 0.15f;
            case NASCENT_SOUL -> 0.12f;
            case SOUL_TRANSFORMATION -> 0.2f;
            case VOID_REFINEMENT -> 0.18f;
            case UNITY -> 0.25f;
            case MAHAYANA -> 0.22f;
            case TRIBULATION -> 0.3f;
            case TRUE_IMMORTAL -> 0.28f;
        };
    }

    /**
     * 获取飞行速度基准（方块/秒）
     * @param realm 境界
     * @return 飞行速度基准
     */
    public static float getFlyingSpeedBase(Realm realm) {
        return switch (realm) {
            case QI_REFINING -> 5.0f;
            case FOUNDATION_ESTABLISHMENT -> 8.0f;
            case CORE_FORMATION -> 12.0f;
            case NASCENT_SOUL -> 18.0f;
            case SOUL_TRANSFORMATION -> 26.0f;
            case VOID_REFINEMENT -> 38.0f;
            case UNITY -> 55.0f;
            case MAHAYANA -> 80.0f;
            case TRIBULATION -> 120.0f;
            case TRUE_IMMORTAL -> 180.0f;
        };
    }
}
