package robbery.quest;

public class QuestProgress {
    private int itemsCompleted;
    boolean completed; // per-quest completion
    private boolean halfRewardGiven; // did we give the first half?

    public QuestProgress(String questId){
        this.itemsCompleted = 0;
        this.completed = false;
        this.halfRewardGiven = false;
    }

    public int getItemsCompleted(){ return itemsCompleted; }
    public void incrementBy(int n){ itemsCompleted += n; }
    public boolean isCompleted(int target){ return itemsCompleted >= target; }
    public void markCompleted(){ this.completed = true; }
    public boolean isHalfRewardGiven(){ return halfRewardGiven; }
    public void setHalfRewardGiven(boolean v){ halfRewardGiven = v; }
    public boolean getCompleted(){return completed;}

    public void setItemsCompleted(int itemsStolen) {
        this.itemsCompleted = itemsStolen;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
