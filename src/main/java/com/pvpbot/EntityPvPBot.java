package com.pvpbot;

import java.util.LinkedList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityPvPBot extends EntityLiving {

    /**
     * Alcance de ataque corpo a corpo de um jogador no cliente vanilla 1.8.9:
     * 3.0 blocos medidos do olho ate a hitbox do alvo (EntityRenderer.getMouseOver,
     * quando nao ha "extendedReach"). E o UNICO valor usado, em todas as dificuldades.
     */
    public static final double REACH = 3.0D;

    private Difficulty difficulty = Difficulty.INTERMEDIARIO;
    private boolean paused = false;

    private final LinkedList<double[]> history = new LinkedList<double[]>();
    private float aimPitch = 0.0F;
    private float aimOffYaw = 0.0F, aimOffPitch = 0.0F;
    private int noiseTimer = 0;
    private int strafeTimer = 0, strafeDir = 1;
    private boolean strafeOn = false;
    private int hesitate = 0;
    private int attackCooldown = 0;
    private int sprintBlock = 0;
    private int wtapPause = 0;
    private int chaseTicks = 0;
    private int stunTicks = 0;

    public EntityPvPBot(World world) {
        super(world);
        this.setSize(0.6F, 1.8F);
        this.isImmuneToFire = true;
        // Desativa os helpers de IA vanilla que zerariam o movimento controlado por este bot.
        this.moveHelper = new EntityMoveHelper(this) {
            @Override
            public void onUpdateMoveHelper() { }
        };
        this.jumpHelper = new EntityJumpHelper(this) {
            @Override
            public void doJump() { }
        };
        this.enablePersistence();
        this.setAlwaysRenderNameTag(true);
        this.updateName();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1024.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.1D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0D);
    }

    // ------------------------------------------------------------------ configuracao

    public Difficulty getDifficulty() { return difficulty; }

    public void setDifficulty(Difficulty d) {
        this.difficulty = d;
        this.updateName();
    }

    public boolean isPaused() { return paused; }

    public void setPaused(boolean p) { this.paused = p; }

    private void updateName() {
        this.setCustomNameTag("\u00a7cBot \u00a77[\u00a7f" + difficulty.display + "\u00a77]");
    }

    /** tipo: none, leather, gold, chain, iron, diamond */
    public void setArmorType(String type) {
        Item[] set = null;
        if ("leather".equals(type)) {
            set = new Item[] { Items.leather_boots, Items.leather_leggings, Items.leather_chestplate, Items.leather_helmet };
        } else if ("gold".equals(type)) {
            set = new Item[] { Items.golden_boots, Items.golden_leggings, Items.golden_chestplate, Items.golden_helmet };
        } else if ("chain".equals(type)) {
            set = new Item[] { Items.chainmail_boots, Items.chainmail_leggings, Items.chainmail_chestplate, Items.chainmail_helmet };
        } else if ("iron".equals(type)) {
            set = new Item[] { Items.iron_boots, Items.iron_leggings, Items.iron_chestplate, Items.iron_helmet };
        } else if ("diamond".equals(type)) {
            set = new Item[] { Items.diamond_boots, Items.diamond_leggings, Items.diamond_chestplate, Items.diamond_helmet };
        }
        for (int i = 0; i < 4; i++) {
            // slots: 1 botas, 2 calca, 3 peitoral, 4 capacete
            this.setCurrentItemOrArmor(i + 1, set == null ? null : new ItemStack(set[i]));
        }
    }

    /** tipo: wood, stone, gold, iron, diamond */
    public void setSwordType(String type) {
        Item sword = Items.diamond_sword;
        if ("wood".equals(type)) sword = Items.wooden_sword;
        else if ("stone".equals(type)) sword = Items.stone_sword;
        else if ("gold".equals(type)) sword = Items.golden_sword;
        else if ("iron".equals(type)) sword = Items.iron_sword;
        this.setCurrentItemOrArmor(0, new ItemStack(sword));
    }

    // ------------------------------------------------------------------ imortalidade

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Void (e /kill): o bot e apagado, nao volta.
        if (source == DamageSource.outOfWorld) {
            if (!this.worldObj.isRemote) this.setDead();
            return false;
        }
        // So jogadores o atingem (queda, fogo, afogamento etc. nao fazem nada).
        if (source == null || !(source.getEntity() instanceof EntityPlayer)) return false;

        // Dano, knockback, flash vermelho e som funcionam como em qualquer mob...
        boolean hurt = super.attackEntityFrom(source, amount);
        if (!this.worldObj.isRemote) {
            // ...mas a vida volta ao maximo: o bot nunca morre.
            this.setHealth(this.getMaxHealth());
            if (hurt) this.stunTicks = this.difficulty.recoveryTicks;
        }
        return hurt;
    }

    // ------------------------------------------------------------------ IA

    @Override
    public float getEyeHeight() {
        return 1.62F; // altura dos olhos de um jogador
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.worldObj.isRemote) {
            if (this.posY < 0.0D) { // caiu no void: remove
                this.setDead();
                return;
            }
            this.rotationPitch = this.aimPitch;       // o LookHelper vanilla zera o pitch
            this.rotationYawHead = this.rotationYaw;
            if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
            this.extinguish();
        }
    }

    private EntityPlayer findTarget() {
        EntityPlayer best = null;
        double bestD = 64.0D * 64.0D;
        for (EntityPlayer p : this.worldObj.playerEntities) {
            if (p.isDead || p.isSpectator() || p.getHealth() <= 0.0F) continue;
            double d = this.getDistanceSqToEntity(p);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        this.moveForward = 0.0F;
        this.moveStrafing = 0.0F;
        this.setJumping(false);

        EntityPlayer target = this.paused ? null : this.findTarget();

        if (target == null) {
            this.setSprinting(false);
            this.history.clear();
            this.applySpeed();
            return;
        }

        Difficulty d = this.difficulty;

        // ---- mira (com atraso de reacao, giro limitado e desvio) ----
        history.addFirst(new double[] { target.posX, target.posY, target.posZ });
        while (history.size() > 24) history.removeLast();
        double[] aim = history.get(Math.min(d.reactionTicks, history.size() - 1));

        if (--noiseTimer <= 0) {
            noiseTimer = d.noiseRefresh;
            aimOffYaw = (float) this.rand.nextGaussian() * d.aimNoise;
            aimOffPitch = (float) this.rand.nextGaussian() * d.aimNoise * 0.6F;
        }

        double dx = aim[0] - this.posX;
        double dz = aim[2] - this.posZ;
        double dy = (aim[1] + target.height * 0.55D) - (this.posY + this.getEyeHeight());
        double hd = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F + aimOffYaw;
        float wantPitch = (float) (-(Math.atan2(dy, hd) * 180.0D / Math.PI)) + aimOffPitch;

        float dYaw = MathHelper.wrapAngleTo180_float(wantYaw - this.rotationYaw);
        this.rotationYaw += MathHelper.clamp_float(dYaw * d.aimSmooth, -d.maxTurn, d.maxTurn);
        float dPitch = wantPitch - this.aimPitch;
        this.aimPitch += MathHelper.clamp_float(dPitch * d.aimSmooth, -d.maxTurn, d.maxTurn);
        this.aimPitch = MathHelper.clamp_float(this.aimPitch, -90.0F, 90.0F);
        this.rotationPitch = this.aimPitch;
        this.rotationYawHead = this.rotationYaw;

        if (stunTicks > 0) stunTicks--;
        boolean stunned = stunTicks > 0;

        // ---- movimento ----
        double ddx = target.posX - this.posX;
        double ddz = target.posZ - this.posZ;
        double hDist = Math.sqrt(ddx * ddx + ddz * ddz);

        if (hesitate > 0) {
            hesitate--;
        } else if (this.rand.nextFloat() < d.hesitateChance) {
            hesitate = 10 + this.rand.nextInt(16);
        }
        boolean hesitating = hesitate > 0 && hDist < 6.0D;

        float forward = 0.0F;
        if (!hesitating) {
            if (hDist > d.stopDistance) forward = 1.0F;
            else if (hDist < 1.0D) forward = -0.6F;
        }
        if (chaseTicks > 0) {
            chaseTicks--;
            if (hDist > 1.2D) forward = 1.0F; // combo: fica colado no alvo
        }
        if (wtapPause > 0) {           // w-tap: solta o W por instantes
            wtapPause--;
            forward = 0.0F;
        }
        if (stunned) forward = 0.0F;   // acabou de apanhar: demora para reagir

        if (--strafeTimer <= 0) {
            strafeTimer = d.strafeMin + this.rand.nextInt(d.strafeMax - d.strafeMin + 1);
            strafeDir = this.rand.nextBoolean() ? 1 : -1;
            strafeOn = this.rand.nextFloat() < d.strafeChance;
        }
        float strafe = (strafeOn && hDist < 5.0D && !hesitating) ? strafeDir * d.strafeStrength : 0.0F;
        if (chaseTicks > 0) strafe *= 0.4F;
        if (stunned) strafe = 0.0F;

        if (sprintBlock > 0) sprintBlock--;
        boolean sprint = forward > 0.0F && !stunned && sprintBlock == 0 && !this.isCollidedHorizontally;
        this.setSprinting(sprint);

        boolean jump = (this.isCollidedHorizontally && this.onGround) || this.isInWater() || this.isInLava();
        if (!jump && this.onGround && !stunned && forward > 0.0F && hDist > 1.5D && hDist < 4.0D
                && this.rand.nextFloat() < d.jumpChance) {
            jump = true; // pulo para acertar critico
        }
        this.setJumping(jump);

        this.moveForward = forward;
        this.moveStrafing = strafe;
        this.applySpeed();

        // ---- ataque ----
        if (attackCooldown > 0) attackCooldown--;
        if (!stunned && attackCooldown <= 0 && hDist < REACH + 1.0D && this.rand.nextFloat() < d.attackChance) {
            attackCooldown = d.cdMin + this.rand.nextInt(d.cdMax - d.cdMin + 1);
            this.swingItem();
            if (this.canReach(target)) {
                this.hit(target);
            }
        }
    }

    private void applySpeed() {
        this.jumpMovementFactor = this.isSprinting() ? 0.026F : 0.02F;
        this.setAIMoveSpeed((float) this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
    }

    /**
     * Verificacao de alcance equivalente a de um jogador (EntityRenderer.getMouseOver):
     * raio saindo do olho na direcao do olhar, comprimento REACH (3.0), contra a hitbox do alvo
     * expandida por getCollisionBorderSize, sem nenhum bloco no caminho.
     * Se a mira nao aponta para a hitbox, ou o alvo esta alem de 3.0 blocos, nao ha ataque.
     */
    private boolean canReach(EntityLivingBase t) {
        Vec3 eye = this.getPositionEyes(1.0F);
        Vec3 look = this.getLook(1.0F);
        Vec3 end = eye.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        float b = t.getCollisionBorderSize();
        AxisAlignedBB box = t.getEntityBoundingBox().expand(b, b, b);

        Vec3 hit;
        if (box.isVecInside(eye)) {
            hit = eye;
        } else {
            MovingObjectPosition mop = box.calculateIntercept(eye, end);
            if (mop == null || mop.hitVec == null) return false;
            hit = mop.hitVec;
        }
        if (eye.distanceTo(hit) > REACH) return false;

        MovingObjectPosition block = this.worldObj.rayTraceBlocks(eye, hit, false, true, false);
        return block == null;
    }

    private void hit(EntityPlayer target) {
        float dmg = (float) this.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        DamageSource src = new EntityDamageSource("player", this) {
            @Override
            public boolean isDifficultyScaled() {
                return false; // mesmo dano de um jogador, independente da dificuldade do mundo
            }
        };
        // Critico vanilla: caindo, fora do chao/escada/agua => +50% de dano.
        if (this.fallDistance > 0.0F && !this.onGround && !this.isOnLadder() && !this.isInWater()) {
            dmg *= 1.5F;
        }
        int kb = this.isSprinting() ? 1 : 0;
        if (target.attackEntityFrom(src, dmg)) {
            if (kb > 0) { // knockback extra de sprint, igual ao do jogador vanilla
                target.addVelocity(
                        -MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F) * kb * 0.5F, 0.1D,
                        MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F) * kb * 0.5F);
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
                this.setSprinting(false);
            }
            Difficulty d = this.difficulty;
            this.sprintBlock = d.sprintBlockMin + this.rand.nextInt(d.sprintBlockMax - d.sprintBlockMin + 1);
            this.wtapPause = d.wtapPause;
            this.chaseTicks = d.comboChaseTicks;
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("PvPDifficulty", difficulty.name());
        tag.setBoolean("PvPPaused", paused);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        try {
            this.difficulty = Difficulty.valueOf(tag.getString("PvPDifficulty"));
        } catch (IllegalArgumentException e) {
            this.difficulty = Difficulty.INTERMEDIARIO;
        }
        this.paused = tag.getBoolean("PvPPaused");
        this.enablePersistence();
        this.updateName();
    }
}
