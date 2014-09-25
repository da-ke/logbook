/**
 * 
 */
package logbook.gui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import logbook.constants.AppConstants;
import logbook.dto.AirBattleDto;
import logbook.dto.BattleAtackDto;
import logbook.dto.BattleExDto;
import logbook.dto.DockDto;
import logbook.dto.ShipDto;
import logbook.internal.EnemyData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * @author Nekopanda
 *
 */
public class BattleWindow extends BattleWindowBase {

    // 名前
    protected final Label[] enemyLabels = new Label[6];

    protected final Label[][] infoLabels = new Label[2][12];

    protected Label matchLabel;
    protected final Label resultLabel[] = new Label[3];

    protected final int[] yDamages = new int[12];
    protected final int[][] friendDamages = new int[2][12];
    protected final int[][] enemyDamages = new int[2][6];

    protected static String AFTER_DAY = "昼戦後";
    protected static String AFTER_NIGHT = "夜戦後";
    protected static String FORM_PREFIX = "陣形:";
    protected static String TOUCH_PREFIX = "触接:";
    protected static String SAKUTEKI_PREFIX = "索敵:";

    /**
     * Create the dialog.
     * @param parent
     */
    protected BattleWindow(Shell parent, MenuItem menuItem) {
        super(parent, menuItem, "戦況");
    }

    protected void clearText() {
        // 情報
        for (int i = 0; i < 12; ++i) {
            this.infoLabels[0][i].setText("");
            this.infoLabels[1][i].setText("");
        }

        // 敵
        for (int i = 0; i < 6; ++i) {
            this.enemyLabels[i].setText("-");
        }

        // その他
        this.matchLabel.setText("");
        this.resultLabel[0].setText("");
        this.resultLabel[1].setText("");
        this.resultLabel[2].setText("");
    }

    protected static void setLabelRed(Label label) {
        label.setBackground(SWTResourceManager.getColor(AppConstants.COND_RED_COLOR));
        label.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
    }

    protected static void setLabelOrange(Label label) {
        label.setBackground(SWTResourceManager.getColor(AppConstants.COND_ORANGE_COLOR));
        label.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
    }

    protected static void setLabelGreen(Label label) {
        label.setBackground(SWTResourceManager.getColor(AppConstants.COND_GREEN_COLOR));
        label.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
    }

    protected static void setLabelNone(Label label) {
        label.setBackground(null);
        label.setForeground(null);
    }

    protected static void printHp(Label label, int nowhp, int maxhp) {
        label.setText(String.valueOf(nowhp) + "/" + maxhp);
    }

    protected void printDock() {
        List<DockDto> docks = this.getDocks();
        if (docks == null)
            return;

        this.infoLabels[0][0].setText(docks.get(0).getName());
    }

    protected void printMap() {
        if (this.getMapCellDto() == null)
            return;

        EnemyData enemyData = this.getMapCellDto().getEnemyData();
        if (enemyData != null) {
            String name = enemyData.getEnemyName();
            if (name != null) {
                this.infoLabels[1][0].setText(name);
            }
            String[] ships = enemyData.getEnemyShips();
            for (int i = 0; i < 6; ++i) {
                this.enemyLabels[i].setText(String.valueOf(i + 1) + "." + ships[i]);
            }
            this.infoLabels[1][1].setText(FORM_PREFIX + enemyData.getFormation());
        }
    }

    private String toPlaneCount(int lost, int total) {
        int after = total - lost;
        return String.valueOf(total) + "→" + after /*+ "(-" + lost + ")"*/;
    }

    // idx: 味方=0, 敵=1
    private void printPlaneCount(Label[] labels, int base, AirBattleDto air, int idx) {
        labels[base + 0].setText("");
        labels[base + 1].setText("");
        if (air != null) {
            if (air.stage1 != null)
                labels[base + 0].setText(this.toPlaneCount(air.stage1[(idx * 2) + 0], air.stage1[(idx * 2) + 1]));
            if (air.stage2 != null)
                labels[base + 1].setText(this.toPlaneCount(air.stage2[(idx * 2) + 0], air.stage2[(idx * 2) + 1]));
        }
    }

    private int computeDamages(int[] friend, int[] enemy, int[] ydam, BattleExDto.Phase phase) {
        BattleAtackDto[][] sequence = phase.getAtackSequence();

        for (int i = 0; i < friend.length; ++i)
            friend[i] = 0;
        for (int i = 0; i < enemy.length; ++i)
            enemy[i] = 0;
        int airDamage = 0;

        for (BattleAtackDto[] atacks : sequence) {
            if (atacks != null) {
                for (BattleAtackDto dto : atacks) {
                    for (int i = 0; i < dto.target.length; ++i) {
                        int target = dto.target[i];
                        int damage = dto.damage[i];
                        if (dto.friendAtack) {
                            enemy[target] += damage;
                        }
                        else {
                            friend[target] += damage;
                        }
                    }
                    if (dto.friendAtack) {
                        switch (dto.kind) {
                        case HOUGEKI:
                            for (int damage : dto.damage) {
                                ydam[dto.origin[0]] += damage;
                            }
                            break;
                        case RAIGEKI:
                            for (int i = 0; i < dto.origin.length; ++i) {
                                ydam[dto.origin[i]] += dto.ydam[i];
                            }
                            break;
                        case AIR:
                            for (int damage : dto.damage) {
                                airDamage += damage;
                            }
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        }

        return airDamage;
    }

    protected static class MVPShip {
        public ShipDto ship;
        public int ydam;

        MVPShip(ShipDto ship, int ydam) {
            this.ship = ship;
            this.ydam = ydam;
        }
    }

    /**
     * MVP候補を計算
     * @param ydam
     * @param ships
     * @return
     */
    private MVPShip[] computeMVP(int[] ydam, List<ShipDto> ships) {
        MVPShip[] sortArray = new MVPShip[ships.size()];
        for (int i = 0; i < ships.size(); ++i) {
            sortArray[i] = new MVPShip(ships.get(i), this.yDamages[i]);
        }
        Arrays.sort(sortArray, new Comparator<MVPShip>() {
            @Override
            public int compare(MVPShip d1, MVPShip d2) {
                return -Integer.compare(d1.ydam, d2.ydam);
            }
        });
        int numPrintShips = 0;
        for (int i = 0; (i < 2) && (i < ships.size()); ++i) {
            if (sortArray[i].ydam == 0) {
                break;
            }
            numPrintShips++;
        }
        if (numPrintShips == 0) {
            numPrintShips = 1;
        }
        return Arrays.copyOf(sortArray, numPrintShips);
    }

    protected String getMVPText(MVPShip[] mvp, int airDamage) {
        if (mvp == null) {
            return "";
        }
        String result0 = "MVP(砲雷のみ) ";
        for (int i = 0; i < mvp.length; ++i) {
            ShipDto ship = mvp[i].ship;
            result0 += String.format("%d: %s(%d)", i + 1,
                    (ship == null) ? "?" : ship.getName(), mvp[i].ydam);
            if (i != (mvp.length - 1))
                result0 += ", ";
        }
        result0 += " 航空戦ダメージ: " + airDamage;
        return result0;
    }

    protected String getReulstText(double[] damageRate, String rank) {
        String rateString = (damageRate[0] == 0.0) ? "" :
                String.format(" (x%.3f)", damageRate[1] / damageRate[0]);
        return String.format("損害率 自: %.1f%% vs. 敵: %.1f%%%s 結果: %s",
                damageRate[0] * 100, damageRate[1] * 100, rateString, rank);
    }

    protected void printBattle() {
        BattleExDto battle = this.getBattle();
        BattleExDto.Phase phase1 = battle.getPhase1();
        BattleExDto.Phase phase2 = battle.getPhase2();
        BattleExDto.Phase lastPhase = battle.getLastPhase();
        List<ShipDto> friendShips = battle.getDock().getShips();
        List<ShipDto> friendShipsCombined = battle.isCombined() ? battle.getDockCombined().getShips() : null;

        if (lastPhase == null)
            return;

        // ダメージ計算
        int airDamage = 0;
        for (int i = 0; i < this.yDamages.length; ++i)
            this.yDamages[i] = 0;
        if (phase1 != null)
            airDamage += this.computeDamages(this.friendDamages[0], this.enemyDamages[0], this.yDamages, phase1);
        if (phase2 != null)
            airDamage += this.computeDamages(this.friendDamages[1], this.enemyDamages[1], this.yDamages, phase2);
        MVPShip[] mvp1 = this.computeMVP(Arrays.copyOf(this.yDamages, friendShips.size()), friendShips);
        MVPShip[] mvp2 = battle.isCombined() ? this.computeMVP(
                Arrays.copyOfRange(this.yDamages, 6, 6 + friendShipsCombined.size()), friendShipsCombined) : null;

        // 情報表示
        String[] formation = battle.getFormation();
        int[] touchPlane = lastPhase.getTouchPlane();
        String[] sakuteki = battle.getSakuteki();
        String seiku = lastPhase.getSeiku();
        AirBattleDto[] air = lastPhase.getAirBattleDto();
        double[] damageRate = lastPhase.getDamageRate();

        for (int i = 0; i < 2; ++i) {
            if (formation[i] != null)
                this.infoLabels[i][1].setText(FORM_PREFIX + formation[i]);
            if (touchPlane != null)
                this.infoLabels[i][2].setText(TOUCH_PREFIX + ((touchPlane[i] != -1) ? "あり" : "なし"));
            if (sakuteki != null)
                this.infoLabels[i][3].setText(SAKUTEKI_PREFIX + sakuteki[i]);
            if (i == 0) {
                this.infoLabels[i][4].setText("航空戦:");
                this.infoLabels[i][5].setText((seiku != null) ? seiku : "なし");
            }
            this.infoLabels[i][6].setText("stage1");
            this.infoLabels[i][7].setText("stage2");
            if (air != null) {
                this.printPlaneCount(this.infoLabels[i], 8, air[0], i);
                this.printPlaneCount(this.infoLabels[i], 10, air[1], i);
            }
        }

        this.matchLabel.setText(battle.getFormationMatch());

        this.resultLabel[0].setText(this.getMVPText(mvp1, airDamage));
        this.resultLabel[1].setText(this.getMVPText(mvp2, 0)); // 第二艦隊は航空戦ダメージゼロ
        this.resultLabel[2].setText(this.getReulstText(damageRate, lastPhase.getEstimatedRank().toString()));

    }
}