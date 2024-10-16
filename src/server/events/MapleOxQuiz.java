/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.events;

import client.MapleCharacter;
import client.MapleStat;
import server.Timer.EventTimer;
import server.events.MapleOxQuizFactory.MapleOxQuizEntry;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class MapleOxQuiz extends MapleEvent {

    private ScheduledFuture<?> oxSchedule, oxSchedule2;
    private int timesAsked = 0;
    private boolean finished = false;

    public MapleOxQuiz(final int channel, final MapleEventType type) {
        super(channel, type);
    }

    @Override
    public void finished(MapleCharacter chr) { //do nothing.
    }

    private void resetSchedule() {
        if (oxSchedule != null) {
            oxSchedule.cancel(false);
            oxSchedule = null;
        }
        if (oxSchedule2 != null) {
            oxSchedule2.cancel(false);
            oxSchedule2 = null;
        }
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        super.onMapLoad(chr);
        if (chr.getMapId() == type.mapids[0] && !chr.isGM()) {
            chr.canTalk(false);
        }
    }

    @Override
    public void reset() {
        super.reset();
        getMap(0).getPortal("join00").setPortalState(false);
        resetSchedule();
        timesAsked = 0;
    }

    @Override
    public void unreset() {
        super.unreset();
        getMap(0).getPortal("join00").setPortalState(true);
        resetSchedule();
    }
    //apparently npc says 10 questions

    @Override
    public void startEvent() {
        sendQuestion();
        finished = false;
    }

    public void sendQuestion() {
        sendQuestion(getMap(0));
    }

    public void sendQuestion(final MapleMap toSend) {
        final Entry<Pair<Integer, Integer>, MapleOxQuizEntry> question = MapleOxQuizFactory.getInstance().grabRandomQuestion();
        if (oxSchedule2 != null) {
            oxSchedule2.cancel(false);
        }
        if (timesAsked == 6) {
            toSend.broadcastMessage(MaplePacketCreator.serverNotice(0, "축하합니다~ 준비된 퀴즈를 모두 맞추셨습니다! 잠시 후 이벤트 상품을 지급받을 수 있습니다."));
        } else {
            getMap(0).broadcastMessage(MaplePacketCreator.serverNotice(0, "20초 후 문제가 출제됩니다. 모두 준비해 주세요!"));
        }
        getMap(0).broadcastMessage(MaplePacketCreator.getClock(20));
        oxSchedule2 = EventTimer.getInstance().schedule(new Runnable() {
            public void run() {
                int number = 0;
                for (MapleCharacter mc : toSend.getCharactersThreadsafe()) {
                    if (mc.isGM() || !mc.isAlive()) {
                        number++;
                    }
                }
                if (toSend.getCharactersSize() - number <= 0 || timesAsked == 6) {
                    unreset();
                    for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                        if (chr != null && !chr.isGM() && chr.isAlive()) {
                            chr.canTalk(true);
//                            chr.finishAchievement(19);
                            winOut(chr);
                        }
                    }
                    //prizes here
                    finished = true;
                    return;
                }

                toSend.broadcastMessage(MaplePacketCreator.stopClock());
                toSend.broadcastMessage(MaplePacketCreator.showOXQuiz(question.getKey().left, question.getKey().right, true));
//                toSend.broadcastMessage(MaplePacketCreator.yellowChat("Answer : " + question.getValue().getAnswer()));
//                toSend.broadcastMessage(MaplePacketCreator.getClock(10)); //quickly change to 12
            }
        }, 20000);
        if (oxSchedule != null) {
            oxSchedule.cancel(false);
        }
        oxSchedule = EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (finished) {
                    return;
                }
                toSend.broadcastMessage(MaplePacketCreator.showOXQuiz(question.getKey().left, question.getKey().right, false));
                timesAsked++;
                for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                    if (chr != null && !chr.isGM() && chr.isAlive()) { // make sure they aren't null... maybe something can happen in 12 seconds.
                        if (!isCorrectAnswer(chr, question.getValue().getAnswer())) {
                            chr.getStat().setHp((short) 0, chr);
                            chr.updateSingleStat(MapleStat.HP, 0);
                        } else {
                            chr.gainExp(3000, true, true, false);
                        }
                    }
                }
                sendQuestion();
            }
        }, 50000); // Time to answer = 30 seconds ( Ox Quiz packet shows a 30 second timer.
    }

    private boolean isCorrectAnswer(MapleCharacter chr, int answer) {
        double x = chr.getTruePosition().getX();
        double y = chr.getTruePosition().getY();
        if ((x > -234 && y > -26 && answer == 0) || (x < -234 && y > -26 && answer == 1)) {
            chr.dropMessage(6, "[OX퀴즈] 정답을 맞추었습니다!"); //i think this is its own packet
            return true;
        }
        chr.dropMessage(6, "[OX퀴즈] 틀렸습니다!");
        return false;
    }
}
