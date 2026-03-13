package robbery.attribute;

import java.util.Set;

public class Attribute {
    public static final String ATTR_MONEY_MULT = "outpost_money_multiplier";
    public static final String ATTR_SKILLPOINT_CHANCE = "skillpoint_chance";
    public static final String ATTR_BOOSTER_CHANCE = "booster_chance";
    public static final String ATTR_SPEED_BONUS = "speed_bonus";

    public static final String PERK_MONEY_MULT1 = "moneymultiplier1";
    public static final String PERK_STEAL_SPEED1 = "stealspeed1";
    public static final String PERK_XP1 = "xp1";
    public static final String PERK_OUT_BUFF1 = "outpostbuff1";
    public static final String PERK_BACK_SLOTS1 = "extrabackpackslots1";
    public static final String PERK_CHANCE_SP1 = "chanceskillpoint1";
    public static final String PERK_ABILITY_MONEYMULT1 = "chancemoneymultiplier1";
    public static final String PERK_BOOST1 = "chancebooster1";
    public static final String PERK_ABILITY_STEALSPEED1 = "chancestealspeed1";
    public static final String PERK_ITEM_STREAK1 = "itemstreakspeed1";
    public static final String PERK_DOUBLE_ITEM1 = "doubleitemchance1";
    public static final String PERK_AVOID_CAUGHT1 = "avoidbeingcaught";
    public static final String PERK_STEAL_SPEED2 = "stealspeed2";
    public static final String PERK_MONEY_MULT2 = "moneymultiplier2";
    public static final String PERK_XP2 = "xp2";
    public static final String PERK_TRIPLE_ITEM1 = "tripleitemchance1";
    public static final String PERK_INSTA_STEAL1 = "instastealchance1";
    public static final String PERK_DOUBLE_INV1 = "doubleinventorychance1";
    public static final String PERK_SPECIAL_KEYCHANCE = "keyschance";

    public static final String PERK_ABILITY_SPQUEST1 = "abilityspinquests1";
    public static final String PERK_ABILITY_SPQUEST2 = "abilityspinquests2";
    public static final String PERK_SPECIAL_DOUBLEJUMP = "doublejump";
    public static final String PERK_SPECIAL_FEATHERFLIGHT = "featherflight";

    public static final Set<String> PERCENTAGE_PERKS = Set.of(
            "chancebooster1",
            "instastealchance1",
            "avoidbeingcaught",
            "doubleinventorychance1",
            "chanceskillpoint1",
            "outpostbuff1",
            "xp1",
            "xp2",
            "doubleitemchance1",
            "tripleitemchance1"
    );


}
