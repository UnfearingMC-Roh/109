package server.maps;

import java.awt.Point;
import client.MapleCharacter;
import handling.world.World;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;

public class AramiaFireWorks {

    public final static int KEG_ID = 4001128, SUN_ID = 4001246, DEC_ID = 4001473;
    public final static int MAX_KEGS = 2400, MAX_SUN = 2000, MAX_DEC = 3600;
    private short kegs = MAX_KEGS / 6;
    private short sunshines = MAX_SUN / 6; //start at 1/6 then go from that
    private short decorations = MAX_DEC / 6;
    private static final int[] arrayMob = {9500168, 9500169, 9500170, 9500171, 9500173,
        9500174, 9500175, 9500176, 9500170, 9500171, 9500172, 9500173, 9500174, 9500175,
        9400569};
    private static final int[] arrayX = {2100, 2605, 1800, 2600, 3120, 2700, 2320, 2062,
        2800, 3100, 2300, 2840, 2700, 2320, 1950};
    private static final int[] arrayY = {574, 364, 574, 316, 574, 574, 403, 364, 574, 574,
        403, 574, 574, 403, 574};
    private static final int[] array_X = {720, 180, 630, 270, 360, 540, 450, 142,
        142, 218, 772, 810, 848, 232, 308, 142};
    private static final int[] array_Y = {1234, 1234, 1174, 1234, 1174, 1174, 1174, 1260,
        1234, 1234, 1234, 1234, 1234, 1114, 1114, 1140};
    private static final int flake_Y = 149;

    public final void giveKegs(final MapleCharacter c, final int kegs) {
        this.kegs += kegs;
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            broadcastEvent(c);
        }
    }

    public int getSunGage() {
        if (sunshines >= 2000) {
            return 100;
        } else if (sunshines >= 2000) {
            return 90;
        } else if (sunshines >= 1800) {
            return 80;
        } else if (sunshines >= 1600) {
            return 70;
        } else if (sunshines >= 1400) {
            return 60;
        } else if (sunshines >= 1200) {
            return 50;
        } else if (sunshines >= 1000) {
            return 40;
        } else if (sunshines >= 800) {
            return 30;
        } else if (sunshines >= 600) {
            return 20;
        } else if (sunshines >= 400) {
            return 10;
        } else if (sunshines == 200) {
        }
        return 0;
    }

    private final void broadcastServer(final MapleCharacter c, final int itemid) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(5, itemid, "<채널 " + c.getClient().getChannel() + "> " + c.getMap().getMapName() + " : 필요한 " + MapleItemInformationProvider.getInstance().getName(itemid) + "이 모두 모여 단풍나무 축제가 열립니다."));
    }

    public final short getKegsPercentage() {
        return (short) ((kegs / MAX_KEGS) * 10000);
    }

    private final void broadcastEvent(final MapleCharacter c) {
        broadcastServer(c, KEG_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startEvent(c.getClient().getChannelServer().getMapFactory().getMap(100000200));
            }
        }, 10000);
    }

    private final void startEvent(final MapleMap map) {
        map.startMapEffect("누가 시끄럽게 폭죽을 쏘는거야?", 5121010);

        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                spawnMonster(map);
            }
        }, 5000);
    }

    private final void spawnMonster(final MapleMap map) {
        Point pos;

        for (int i = 0; i < arrayMob.length; i++) {
            pos = new Point(arrayX[i], arrayY[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob[i]), pos);
        }
    }

    public final void giveSuns(final MapleCharacter c, final int kegs) {
        this.sunshines += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(970010000);
        final MapleReactor reactor = map.getReactorByName("mapleTree");
        for (int gogo = kegs + (MAX_SUN / 6); gogo > 0; gogo -= (MAX_SUN / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.sunshines >= (MAX_SUN / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.sunshines >= (MAX_SUN / 6)) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.sunshines >= MAX_SUN) {
            this.sunshines = 0;
            broadcastSun(c);
        }
    }

    public final short getSunsPercentage() {
        return (short) ((sunshines / MAX_SUN) * 10000);
    }

    private final void broadcastSun(final MapleCharacter c) {
        broadcastServer(c, SUN_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startSun(c.getClient().getChannelServer().getMapFactory().getMap(970010000));
            }
        }, 2000);
    }

    private final void startSun(final MapleMap map) {
        map.startMapEffect("햇볕과 함께 나무가 나뭇잎을 뿌립니다!",  5120008);
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                spawnItem(map);
            }
        }, 8000);
    }

    private final void spawnItem(final MapleMap map) {
        int[] items = {4001126, 4001126};
        int numItems = 90;
        Point dropPosA = new Point(501, 1170);
        Point dropPosB = new Point(501, 1170);
        Point dropPosS = new Point(501, 950);
        int jA = 0;
        int jB = 0;
        for (int i = 0; i < numItems; i++) {
            if (map.calcPointBelow(new Point(dropPosA.x - 12, dropPosA.y)) != null) {
                if (map.calcPointBelow(new Point(dropPosA.x - 24, dropPosA.y)) != null) {
                    dropPosA.x -= 12;
                } else {
                    jA++;
                }
            } else {
                jA++;
            }
            if (map.calcPointBelow(new Point(dropPosB.x + 12, dropPosB.y)) != null) {
                if (map.calcPointBelow(new Point(dropPosB.x + 24, dropPosB.y)) != null) {
                    dropPosB.x += 12;
                } else {
                    jB++;
                }
            } else {
                jB++;
            }
        }
        boolean CheckA = true, CheckB = true;
        int CheckItemA = 0;
        int CheckItemB = 0;
        for (int i = 0; i < numItems; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
            if (CheckA) {
                int randItem = 4001126;
                if (Math.floor(Math.random() * 10) == 0) {
                    int prop = Randomizer.rand(1, 18);
                    switch (prop) {
                        case 1:
                            randItem = 5060003; //피넛머신
                            break;
                        case 2:
                            randItem = 5060002; //부화기
                            break;
                        case 3:
                            randItem = 5062002; //수미큐
                            break;
                        case 4:
                            randItem = 5060003; //피넛머신
                            break;
                        case 5:
                            randItem = 5060002; //부화기
                            break;
                        case 6:
                            randItem = 2431009; //마일리지1000
                            break;
                        case 7:
                            randItem = 5062001; //마미큐
                            break;
                        case 8:
                            randItem = 5062000; //레큐
                            break;
                        case 9:
                            randItem = 5062002; //수미큐
                            break;
                        case 10:
                            randItem = 2431009; //마일리지1000
                            break;
                        case 11:
                            randItem = 5062000; //레큐
                            break;
                        case 12:
                            randItem = 4001126; //단풍
                            break;
                        case 13:
                            randItem = 4001126; //단풍
                            break;
                        case 14:
                            randItem = 4001126; //단풍
                            break;
                        case 15:
                            randItem = 4310002; //7주년은화
                            break;
                        case 16:
                            randItem = 4310002; //7주년은화
                            break;
                        case 17:
                            randItem = 4310002; //7주년은화
                            break;
                        case 18:
                            randItem = 4310002; //7주년은화
                            break;
                    }
                    CheckItemA++;
                    /*if (CheckItemA > 5) {
                        randItem = 4001126;
                    }*/
                }
                map.spawnAutoDrop(randItem, map.calcDropPos(dropPosA, new Point(dropPosA.x, dropPosA.y + 20)), dropPosS);
            }
            if (CheckB) {
                int randItem = 4001126;
                if (Math.floor(Math.random() * 10) == 0) {
                    int prop = Randomizer.rand(1, 18);
                    switch (prop) {
                        case 1:
                            randItem = 5060003; //피넛머신
                            break;
                        case 2:
                            randItem = 5062002; //수미큐
                            break;
                        case 3:
                            randItem = 5060002; //부화기
                            break;
                        case 4:
                            randItem = 5060003; //피넛머신
                            break;
                        case 5:
                            randItem = 2431009; //마일리지1000
                            break;
                        case 6:
                            randItem = 2431006; //마일리지500
                            break;
                        case 7:
                            randItem = 5062001; //마미큐
                            break;
                        case 8:
                            randItem = 5062000; //레큐
                            break;
                        case 9:
                            randItem = 5062002; //수미큐
                            break;
                        case 10:
                            randItem = 5062002; //수미큐
                            break;
                        case 11:
                            randItem = 5062000; //레큐
                            break;
                        case 12:
                            randItem = 4001126; //단풍
                            break;
                        case 13:
                            randItem = 4001126; //단풍
                            break;
                        case 14:
                            randItem = 4001126; //단풍
                            break;
                        case 15:
                            randItem = 4310002; //7주년은화
                            break;
                        case 16:
                            randItem = 4310002; //7주년은화
                            break;
                        case 17:
                            randItem = 4310002; //7주년은화
                            break;
                        case 18:
                            randItem = 4310002; //7주년은화
                            break;
                    }
                    CheckItemB++;
                    /*if (CheckItemB > 5) {
                        randItem = 4001126;
                    }*/
                }
                map.spawnAutoDrop(randItem, map.calcDropPos(dropPosB, new Point(dropPosB.x, dropPosB.y + 20)), dropPosS);
            }
            if (jA <= 0) {
                if (map.calcPointBelow(new Point(dropPosA.x + 24, dropPosA.y)) != null) {
                    if (map.calcPointBelow(new Point(dropPosA.x + 48, dropPosA.y)) != null) {
                        dropPosA.x += 24;
                        if (dropPosA.x > dropPosS.x) {
                            CheckA = false;
                        }
                    }
                }
            } else {
                jA -= 2;
            }
            if (jB <= 0) {
                if (map.calcPointBelow(new Point(dropPosB.x - 24, dropPosB.y)) != null) {
                    if (map.calcPointBelow(new Point(dropPosB.x - 48, dropPosB.y)) != null) {
                        dropPosB.x -= 24;
                        if (dropPosB.x < dropPosS.x) {
                            CheckB = false;
                        }
                    }
                }
            } else {
                jB -= 2;
            }
        }
        map.resetReactors();
        sunshines = 0;
    }

    public final void giveDecs(final MapleCharacter c, final int kegs) {
        this.decorations += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555000000);
        final MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_DEC / 6); gogo > 0; gogo -= (MAX_DEC / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.decorations >= (MAX_DEC / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.decorations >= MAX_DEC / 6) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.decorations >= MAX_DEC) {
            this.decorations = 0;
            broadcastDec(c);
        }
    }

    public final short getDecsPercentage() {
        return (short) ((decorations / MAX_DEC) * 10000);
    }

    private final void broadcastDec(final MapleCharacter c) {
        broadcastServer(c, DEC_ID);
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startDec(c.getClient().getChannelServer().getMapFactory().getMap(555000000));
            }
        }, 10000); //no msg
    }

    private final void startDec(final MapleMap map) {
        map.startMapEffect("The tree is bursting with snow!", 5120000);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnDec(map);
                }
            }, 2000 + (i * 10000));
        }
    }

    private final void spawnDec(final MapleMap map) {
        Point pos;

        for (int i = 0; i < Randomizer.nextInt(10) + 40; i++) {
            pos = new Point(Randomizer.nextInt(800) - 400, flake_Y);
            map.spawnAutoDrop(Randomizer.nextInt(15) == 1 ? 4310012 : 4310011, pos);
        }
    }
}
