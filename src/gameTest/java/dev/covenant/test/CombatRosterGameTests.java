package dev.covenant.test;

import dev.covenant.test.fixture.TestChunks;
import dev.covenant.test.fixture.TestCleanup;
import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.rpg.skills.*;

import com.google.gson.JsonParser;
import dev.covenant.rpg.*;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Real activations, native food/arrow/damage hooks and loaded-entity fixtures for the three-skill slice. */
public final class CombatRosterGameTests {
    public static void channel(GameTestHelper h) {
        var f=new Fixture(h,180);
        var p=f.player(PlayerClass.STALKER,0); f.learn(p,SkillDefinition.BARRAGE,0); f.bow(p);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->f.run(()->{
            input(p,new SkillNetworking.ChannelInput(0,1,true,true));
            check(Barrage.active(p),"Held start creates channel"); near(f.resource(p),94,"First arrow spends six Focus");
            check(p.getInventory().getItem(1).getCount()==63,"First arrow uses native ammo");
        })).thenIdle(2).thenExecute(()->f.run(()->{
            double next=number(p,"nextBarrageShot");
            input(p,new SkillNetworking.ChannelInput(0,1,false,false));
            check(!Barrage.active(p),"Release stops firing");
            input(p,new SkillNetworking.ChannelInput(0,2,true,true));
            near(f.resource(p),94,"Restart cannot bypass firing interval");
            near(number(p,"nextBarrageShot"),next,"Deadline survives restart");
            input(p,new SkillNetworking.ChannelInput(0,1,false,false));
            check(Barrage.active(p),"Stale release cannot cancel new channel");
        })).thenIdle(20).thenExecute(()->f.run(()->{
            near(f.resource(p),76,"Four arrows across the first second; no Focus regeneration while held");
            check(p.getInventory().getItem(1).getCount()==60,"Each fired arrow consumes one ammo");
            check(Skills.cooldownRemaining(p,SkillDefinition.BARRAGE)==0,"No skill cooldown");
            ClassResources.set(p,PlayerClass.STALKER,0); channelTick(p);
            check(!Barrage.active(p),"Insufficient Focus ends channel");
            ClassResources.set(p,PlayerClass.STALKER,100);
            input(p,new SkillNetworking.ChannelInput(0,2,false,true));
            check(!Barrage.active(p),"Heartbeat cannot restart exhausted channel");
            f.close(); h.succeed();
        }));
    }
    public static void channelInterruptions(GameTestHelper h) {
        try (var f=new Fixture(h,190)) { var p=f.player(PlayerClass.STALKER,0);
        f.learn(p,SkillDefinition.BARRAGE,0); f.learn(p,SkillDefinition.PIERCING_SHOT,1);
        check(key(Skills.activate(p,SkillDefinition.BARRAGE),"bow"),"Missing bow rejected without spending");
        f.bow(p); f.use(p,SkillDefinition.BARRAGE);
        Rooted.apply(p,60); channelTick(p); check(Barrage.active(p),"Root permits continued firing");
        p.hurtServer(h.getLevel(),p.damageSources().generic(),1); channelTick(p);
        check(Barrage.active(p),"Ordinary damage leaves channel running");
        p.setData(RpgData.SKILLS,p.getData(RpgData.SKILLS).withCooldown(SkillDefinition.PIERCING_SHOT.id(),ClassResources.now(p)+40));
        check(key(Skills.activate(p,SkillDefinition.PIERCING_SHOT),"cooldown") && Barrage.active(p),"Rejected skill does not interrupt");
        throttle(p);
        p.setData(RpgData.SKILLS,p.getData(RpgData.SKILLS).withCooldown(SkillDefinition.PIERCING_SHOT.id(),0));
        f.use(p,SkillDefinition.PIERCING_SHOT); check(!Barrage.active(p),"Successful other skill ends channel");
        f.use(p,SkillDefinition.BARRAGE);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,net.minecraft.world.InteractionHand.MAIN_HAND));
        check(!Barrage.active(p),"Ordinary item/bow use ends channel");
        p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND); f.use(p,SkillDefinition.BARRAGE);
        check(!p.isUsingItem(),"Channel startup cancels ordinary draw without firing it");
        freeze(p); channelTick(p); check(!Barrage.active(p),"Action prevention ends channel");
        Auras.clear(p); f.use(p,SkillDefinition.BARRAGE);
        SkillLifecycle.cancelPlayer(p,SkillLifecycle.Reason.DIMENSION_CHANGE);
        check(!Barrage.active(p),"Lifecycle ends channel");
        check(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("covenant","barrage_draw"))==null,"Movement modifier removed");
        f.close(); h.succeed();
        }
    }
    public static void firestormCast(GameTestHelper h) {
        var f=new Fixture(h,200); var p=f.player(PlayerClass.WEAVER,0);
        f.learn(p,SkillDefinition.FIRESTORM,0); f.learn(p,SkillDefinition.DISCHARGE,1);
        p.setXRot(45);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->f.run(()->{
            check(key(Skills.activate(p,SkillDefinition.FIRESTORM),"started"),"Firestorm winds up"); near(f.resource(p),100,"No Mana spent before completion");
            p.setYRot(90);
        })).thenIdle(17).thenExecute(()->f.run(()->{
            var storm=deployment(p,"firestorm");
            check(storm!=null && storm.getX()<p.getX()-1,"Ground location uses completion aim");
            near(f.resource(p),60,"Completion spends exactly forty Mana");
            near(WeaverPower.charges(p,WeaverPower.Element.FIRE),1,"One Fire charge even without targets");
            var point=storm.position(); p.setPos(p.position().add(2,0,0)); storm.tick();
            check(storm.position().equals(point),"Storm remains fixed when owner moves");
            SkillLifecycle.cancelPlayer(p,SkillLifecycle.Reason.LOGOUT);
            check(storm.isRemoved(),"Logout removes deployment"); near(f.resource(p),60,"Cleanup gives no refund");
            check(Skills.cooldownRemaining(p,SkillDefinition.FIRESTORM)>0,"Cleanup preserves cooldown");
            f.close();h.succeed();
        }));
    }
    public static void firestormDamage(GameTestHelper h) {
        try (var f=new Fixture(h,210)) { var p=f.player(PlayerClass.WEAVER,0);var q=f.player(PlayerClass.WEAVER,1);
        f.team(p,q);
        for(var owner:List.of(p,q)) { f.learn(owner,SkillDefinition.FIRESTORM,0);owner.getAttribute(RpgStats.CAST_TIME).setBaseValue(0); owner.setXRot(90); f.use(owner,SkillDefinition.FIRESTORM); }
        f.learn(p,SkillDefinition.THREEFOLD_SYSTEM,1);
        var a=deployment(p,"firestorm"); var b=deployment(q,"firestorm");
        var target=f.target(f.origin); var top=f.origin.add(0,4,0);var bottom=f.origin.add(0,-.3,0);long now=ClassResources.now(p);
        target.invulnerableTime=19;
        impact(a,top,bottom,now); near(target.getHealth(),197,"Impact applies three native fire damage");
        check(target.isOnFire(),"Positive impact ignites");check(target.invulnerableTime==19,"Impact preserves independent native hurt timer");
        near(WeaverPower.reserve(p,WeaverPower.Element.ICE),.75,"Confirmed impact banks Threefold once");
        impact(a,top,bottom,now);near(target.getHealth(),197,"Same storm respects per-target interval");
        impact(b,top,bottom,now);near(target.getHealth(),194,"Different caster has independent interval");
        impact(a,top,bottom,now+10);near(target.getHealth(),191,"Same storm can damage after interval");
        f.block(0,2,0,Blocks.STONE);impact(a,top,bottom,now+20);
        near(target.getHealth(),191,"New solid cover stops falling impact and protects lower floor");
        check(!a.hurtServer(h.getLevel(),p.damageSources().generic(),100),"Deployment takes no incidental damage");
        f.close();h.succeed();
        }
    }
    public static void banner(GameTestHelper h) {
        var f=new Fixture(h,220);var p=f.player(PlayerClass.REAVER,0);var q=f.player(PlayerClass.REAVER,1);var ally=f.player(PlayerClass.STALKER,2);
        f.team(p,q,ally);for(var source:List.of(p,q)) { f.learn(source,SkillDefinition.WAR_BANNER,0); }
        ally.setHealth(10);ally.getFoodData().setFoodLevel(12);ally.getFoodData().setSaturation(3);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->f.run(()->{
        for(var source:List.of(p,q)) { f.use(source,SkillDefinition.WAR_BANNER); }
        check(WarBanners.covered(ally),"Banner immediately covers eligible ally");
        check(Auras.supportOwners(ally,false).containsAll(Set.of(p.getUUID(),q.getUUID())),"All overlapping suppliers retain support attribution");
        for(int i=0;i<20;i++) { ally.causeFoodExhaustion(6);ally.getFoodData().tick(ally); }
        near(ally.getFoodData().getFoodLevel(),12,"Action exhaustion cannot drain hunger");near(ally.getFoodData().getSaturationLevel(),3,"Saturation preserved, not restored");
        })).thenIdle(22).thenExecute(()->f.run(()->{
            near(ally.getHealth(),11,"Two banners still heal only one HP per second");
            deployment(p,"banner").discard();check(WarBanners.covered(ally),"Other supplier keeps coverage");
            deployment(q,"banner").discard();check(!WarBanners.covered(ally),"Last supplier cleanup removes coverage");
            for(int i=0;i<5;i++) { ally.getFoodData().tick(ally); }
            near(ally.getFoodData().getSaturationLevel(),3,"No deferred exhaustion after leaving");
            f.close();h.succeed();
        }));
    }
    public static void bannerFood(GameTestHelper h) {
        try(var f=new Fixture(h,230)) {
            var p=f.player(PlayerClass.REAVER,0); f.learn(p,SkillDefinition.WAR_BANNER,0); f.use(p,SkillDefinition.WAR_BANNER);
            p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);p.setHealth(5);
            for(int i=0;i<50;i++) { p.getFoodData().tick(p); }
            check(p.getHealth()>5,"Native food regeneration continues");near(p.getFoodData().getSaturationLevel(),5,"Native internal regeneration exhaustion is prevented");
            deployment(p,"banner").discard();
            p.getFoodData().setFoodLevel(12);for(int i=0;i<8;i++) { p.getFoodData().tick(p); }
            near(p.getFoodData().getSaturationLevel(),5,"Internal regeneration creates no delayed debt");
            h.succeed();
        }
    }
    public static void replacement(GameTestHelper h) {
        try(var f=new Fixture(h,240)) {
            for(var type:List.of(PlayerClass.REAVER,PlayerClass.WEAVER)) {
                var p=f.player(type,type==PlayerClass.REAVER?0:2);var skill=type==PlayerClass.REAVER?SkillDefinition.WAR_BANNER:SkillDefinition.FIRESTORM;
                f.learn(p,skill,0);p.setXRot(90);p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);Skills.setIgnoreCooldowns(p,true);f.use(p,skill);
                var first=type==PlayerClass.REAVER?deployment(p,"banner"):deployment(p,"firestorm");
                double balance=f.resource(p);
                if(type==PlayerClass.REAVER) {p.setOnGround(false);} else {p.setXRot(-90);}
                throttle(p);
                check(key(Skills.activate(p,skill),"invalid_targeting"),"Ground skills reject invalid placement without Teleport feedback");
                check(!first.isRemoved(),"Failed recast preserves deployment");near(f.resource(p),balance,"Failed recast does not spend");
                p.setOnGround(true);p.setXRot(90);ClassResources.set(p,type,100);f.use(p,skill);
                check(first.isRemoved(),"Successful recast replaces previous deployment");
                var replacement=type==PlayerClass.REAVER?deployment(p,"banner"):deployment(p,"firestorm");
                f.learn(p,skill,-1);var previous=p.getData(RpgData.SKILL_LOADOUT);var next=SkillTree.deallocate(previous,skill.id());p.setData(RpgData.SKILL_LOADOUT,next);SkillLifecycle.loadoutChanged(p,previous,next);
                check(replacement.isRemoved(),"Unlearning removes deployment");
            }
            h.succeed();
        }
    }
    public static void config(GameTestHelper h) {
        var root=new com.google.gson.JsonObject();
        try {
            for(var name:List.of("stalker","weaver","reaver")) {
                try(var reader=h.getLevel().getServer().getResourceManager().getResourceOrThrow(net.minecraft.resources.Identifier.fromNamespaceAndPath("covenant","skills/"+name+".json")).openAsReader()) {
                    var json=JsonParser.parseReader(reader).getAsJsonObject();String key=name.equals("stalker")?"barrage":name.equals("weaver")?"firestorm":"war_banner";root.add(key,json.get(key));
                }
            }
        } catch(java.io.IOException e) {throw new IllegalStateException(e);}
        var b=CombatSkillData.parse(root);near(b.barrage().rate(),3,"Barrage proposal rate");near(b.banner().healing(),1,"Banner proposal healing");near(b.firestorm().rate(),6,"Six impacts per second across storm");
        root.getAsJsonObject("firestorm").addProperty("hit_interval_ticks",0);boolean rejected=false;try {CombatSkillData.parse(root);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Invalid hit interval rejected");
        check(SkillDefinition.ALL.size()==39,"Thirty-nine executable skills");
        check(SkillTree.find(SkillDefinition.WAR_BANNER.id()).orElseThrow().prerequisite().isEmpty(),"Banner has no prerequisite");h.succeed();
    }
    public static void reload(GameTestHelper h) {
        var f=new Fixture(h,250);
        var p=f.player(PlayerClass.WEAVER,0);
        h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{
            var server=h.getLevel().getServer();var packs=List.copyOf(server.getPackRepository().getSelectedIds());
            var pack=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("covenant-roster-"+UUID.randomUUID());
            var directory=pack.resolve("data/covenant/skills");
            try(f) {
                try {
                    f.learn(p,SkillDefinition.FIRESTORM,0);p.setXRot(90);p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);Skills.setIgnoreCooldowns(p,true);f.use(p,SkillDefinition.FIRESTORM);
                    var old=deployment(p,"firestorm");var target=f.target(f.origin);var resources=p.getData(RpgData.SKILLS);
                    com.google.gson.JsonObject json;
                    try(var reader=server.getResourceManager().getResourceOrThrow(WeaverSkillData.FILE).openAsReader()){json=JsonParser.parseReader(reader).getAsJsonObject();}
                    json.getAsJsonObject("firestorm").addProperty("damage",8);
                    java.nio.file.Files.createDirectories(directory);
                    java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Temporary skill reload test\",\"min_format\":101,\"max_format\":101}}");
                    java.nio.file.Files.writeString(directory.resolve("weaver.json"),json.toString());
                    server.getPackRepository().reload();var selected=new ArrayList<>(packs);selected.add("file/"+pack.getFileName());server.reloadResources(selected).join();
                    var live=CombatSkillData.get(server);near(live.firestorm().damage(),8,"Class file reload updates Firestorm tuning");
                    check(p.getData(RpgData.SKILLS).equals(resources),"Reload preserves resource and cooldown state");
                    var top=f.origin.add(0,4,0);var bottom=f.origin.add(0,-.3,0);long now=ClassResources.now(p);
                    impact(old,top,bottom,now);near(target.getHealth(),197,"Existing storm retains damage snapshot");
                    ClassResources.set(p,PlayerClass.WEAVER,100);f.use(p,SkillDefinition.FIRESTORM);
                    impact(deployment(p,"firestorm"),top,bottom,now);near(target.getHealth(),189,"New storm adopts updated damage without double scaling");
                    json.getAsJsonObject("firestorm").addProperty("hit_interval_ticks",0);java.nio.file.Files.writeString(directory.resolve("weaver.json"),json.toString());
                    boolean rejected=false;try{server.reloadResources(selected).join();}catch(java.util.concurrent.CompletionException e){rejected=true;}
                    check(rejected && CombatSkillData.get(server)==live,"Invalid reload retains live snapshot");h.succeed();
                } finally {
                    server.reloadResources(packs).join();
                    java.nio.file.Files.deleteIfExists(directory.resolve("weaver.json"));java.nio.file.Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                    java.nio.file.Files.deleteIfExists(directory);java.nio.file.Files.deleteIfExists(directory.getParent());java.nio.file.Files.deleteIfExists(pack.resolve("data"));java.nio.file.Files.deleteIfExists(pack);server.getPackRepository().reload();
                }
            } catch(java.io.IOException e){throw new IllegalStateException(e);}
        });
    }
    private static Object field(Object target,String name) { try {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);} }
    private static double number(ServerPlayer p,String name){return ((Number)field(p.getData(RpgData.SKILL_RUNTIME),name)).doubleValue();}
    private static GroundSkillEntity deployment(ServerPlayer p,String name){return (GroundSkillEntity)field(p.getData(RpgData.SKILL_RUNTIME),name);}
    private static void throttle(ServerPlayer p){try{var f=SkillRuntime.class.getDeclaredField("nextActivation");f.setAccessible(true);f.setLong(p.getData(RpgData.SKILL_RUNTIME),0);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void call(Class<?> type,String name,Class<?>[] types,Object receiver,Object...args){try{var m=type.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(receiver,args);}catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException r)throw r;throw new IllegalStateException(e.getCause());}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void input(ServerPlayer p,SkillNetworking.ChannelInput input){call(Barrage.class,"input",new Class<?>[]{ServerPlayer.class,SkillNetworking.ChannelInput.class},null,p,input);}
    private static void channelTick(ServerPlayer p){call(Barrage.class,"tick",new Class<?>[]{ServerPlayer.class},null,p);}
    private static void freeze(ServerPlayer p){call(Auras.class,"dischargeFreeze",new Class<?>[]{ServerPlayer.class,LivingEntity.class,int.class},null,p,p,20);}
    private static void impact(GroundSkillEntity e,Vec3 from,Vec3 to,long now){call(GroundSkillEntity.class,"impact",new Class<?>[]{Vec3.class,Vec3.class,long.class},e,from,to,now);}
    private static boolean key(Component c,String key) {return c.getContents() instanceof TranslatableContents t && t.getKey().equals("commands.covenant.skill."+key);}
    private static void check(boolean value,String message) {if(!value)throw new IllegalStateException(message);}
    private static void near(double value,double expected,String message) {check(Math.abs(value-expected)<.001,message+": "+value+" != "+expected);}
    private static final class Fixture implements AutoCloseable {
        private boolean cleanupComplete;
        final GameTestHelper h;final Vec3 origin;boolean closed;
        final List<ServerPlayer> players=new ArrayList<>();final List<Entity> entities=new ArrayList<>();
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new HashMap<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams=new ArrayList<>();
        final TestChunks ticket;
        final List<ChunkPos> chunks=new ArrayList<>();
        Fixture(GameTestHelper h,int height) {
            ticket = new TestChunks(h);
            TestCleanup.onFinish(h, this::close);
            this.h=h;origin=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,height,2)));
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)block(x,-1,z,Blocks.STONE);
            for(int x=net.minecraft.util.Mth.floor(origin.x-9)>>4;x<=net.minecraft.util.Mth.floor(origin.x+9)>>4;x++)for(int z=net.minecraft.util.Mth.floor(origin.z-9)>>4;z<=net.minecraft.util.Mth.floor(origin.z+9)>>4;z++) {
                var chunk=new ChunkPos(x,z);chunks.add(chunk);
                ticket.load(chunk);
            }
            h.runBeforeTestEnd(this::close);h.onEachTick(this::tick);
        }
        void ready(){for(var c:chunks)h.assertTrue(h.getLevel().areEntitiesActuallyLoadedAndTicking(c),"Fixture chunks must entity-tick");}
        ServerPlayer player(PlayerClass type,double z) {
            var p = TestPlayers.create(h);
            players.add(p);p.setGameMode(GameType.SURVIVAL);p.connection.markClientLoaded();p.setNoGravity(true);p.setPos(origin.add(0,0,z));p.setOnGround(true);p.setYRot(0);p.setXRot(0);
            PlayerProfiles.selectClass(p,type);ClassResources.set(p,type,100);p.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);ClassResources.markCombat(p);return p;
        }
        void learn(ServerPlayer p,SkillDefinition skill,int slot){var old=p.getData(RpgData.SKILL_LOADOUT);var next=old.withLearned(skill.id(),true);if(slot>=0)next=next.withBinding(slot,skill.id());p.setData(RpgData.SKILL_LOADOUT,next);SkillLifecycle.loadoutChanged(p,old,next);}
        void bow(ServerPlayer p){p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));p.getInventory().setItem(1,new ItemStack(Items.ARROW,64));}
        void use(ServerPlayer p,SkillDefinition skill){throttle(p);var result=Skills.activate(p,skill);check(key(result,"used")||key(result,"started"),"Activation: "+result.getString());}
        double resource(ServerPlayer p){return ClassResources.current(p,PlayerProfiles.getSelectedClass(p).orElseThrow());}
        LivingEntity target(Vec3 pos){var e=EntityType.HUSK.create(h.getLevel(),EntitySpawnReason.COMMAND);e.setPos(pos);e.setNoGravity(true);e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);e.setHealth(200);h.getLevel().addFreshEntity(e);entities.add(e);return e;}
        void block(int x,int y,int z,Block b){var pos=BlockPos.containing(origin).offset(x,y,z);blocks.putIfAbsent(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlockAndUpdate(pos,b.defaultBlockState());}
        void team(ServerPlayer...ps){var board=h.getLevel().getScoreboard();var t=board.addPlayerTeam("s"+UUID.randomUUID().toString().substring(0,8));teams.add(t);for(var p:ps)board.addPlayerToTeam(p.getScoreboardName(),t);}
        void tick(){if(closed)return;for(var p:players){var channel=field(p.getData(RpgData.SKILL_RUNTIME),"channel");if(channel!=null && ((Number)field(channel,"slot")).intValue()>=0)input(p,new SkillNetworking.ChannelInput(((Number)field(channel,"slot")).intValue(),((Number)field(channel,"sequence")).longValue(),false,true));p.doTick();}}
        void run(Runnable action){try{action.run();}catch(RuntimeException|Error e){close();h.fail(e.getMessage()==null?e.toString():e.getMessage());}}
        @Override public void close(){
            if (cleanupComplete) { return; }
            cleanupComplete = true;if(closed)return;closed=true;for(var p:players){entities.addAll(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,p.getBoundingBox().inflate(100)));SkillLifecycle.cancelPlayer(p,SkillLifecycle.Reason.LOGOUT);h.getLevel().getServer().getPlayerList().remove(p);}entities.forEach(Entity::discard);blocks.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));teams.forEach(h.getLevel().getScoreboard()::removePlayerTeam);ticket.close();}
    }
}
