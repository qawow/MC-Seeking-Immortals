package com.xunxian.seekingimmortals.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * M14 伤害管线前后置回调，供 M15 法宝 / M07 阵法场效果接入。
 * <p>全部在服务端裁决路径调用；回调内不得再触发 {@code LivingEntity#hurt} 递归。</p>
 */
public final class DamagePipelineHooks {
    private static final List<Consumer<DamageContext>> PRE_HOOKS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<DamageContext>> POST_HOOKS = new CopyOnWriteArrayList<>();

    private DamagePipelineHooks() {}

    public static final class DamageContext {
        private final LivingEntity attacker;
        private final LivingEntity defender;
        private double amount;
        private boolean canceled;
        private boolean crit;
        private boolean dodged;
        private boolean missed;

        public DamageContext(LivingEntity attacker, LivingEntity defender, double amount) {
            this.attacker = attacker;
            this.defender = defender;
            this.amount = amount;
        }

        public LivingEntity attacker() { return attacker; }
        public LivingEntity defender() { return defender; }
        public double amount() { return amount; }
        public void setAmount(double amount) { this.amount = Math.max(0.0D, amount); }
        public boolean isCanceled() { return canceled; }
        public void cancel() { this.canceled = true; }
        public boolean isCrit() { return crit; }
        public void setCrit(boolean crit) { this.crit = crit; }
        public boolean isDodged() { return dodged; }
        public void setDodged(boolean dodged) { this.dodged = dodged; }
        public boolean isMissed() { return missed; }
        public void setMissed(boolean missed) { this.missed = missed; }
    }

    public static void registerPreHook(Consumer<DamageContext> hook) {
        if (hook != null) {
            PRE_HOOKS.add(hook);
        }
    }

    public static void registerPostHook(Consumer<DamageContext> hook) {
        if (hook != null) {
            POST_HOOKS.add(hook);
        }
    }

    public static void clearHooksForTests() {
        PRE_HOOKS.clear();
        POST_HOOKS.clear();
    }

    public static DamageContext firePre(LivingEntity attacker, LivingEntity defender, double amount) {
        DamageContext context = new DamageContext(attacker, defender, amount);
        for (Consumer<DamageContext> hook : PRE_HOOKS) {
            hook.accept(context);
            if (context.isCanceled()) {
                break;
            }
        }
        return context;
    }

    public static void firePost(DamageContext context) {
        if (context == null) {
            return;
        }
        for (Consumer<DamageContext> hook : POST_HOOKS) {
            hook.accept(context);
        }
    }
}
