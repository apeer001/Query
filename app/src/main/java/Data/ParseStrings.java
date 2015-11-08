package Data;

/**
 * Created by Aaron Peery on 11/8/2015.
 *
 *  This class is to be used as the resource for string values that will be used
 *  to parse questions used in the Query App.
 *
 *  The strings will parse out:
 *      questions terms,
 *      time terms,
 *      action term,
 *      quantative terms
 */
public class ParseStrings {

    public static final String questionTerms[]  = {
        "How Many",
        "What",
        "When",
        "How Much",
        "How Long",
        "Commands",
        "Did I",
        "Do I",
        "Does my",
        "Has my",
        "Have I",
        "How often",
        "which day",
        "Am I",
        "which [other]",
        "How far",
        "Are there",
        "Can You",
        "How has",
        "where",
        "who",
        "Amount of",
        "How Many",
        "How Close",
        "How close",
        "How is my",
        "Does [other]",
        "How active",
        "How are",
        "How can I"
    };

    public static final String timeTerms[]  = {
        "Last",
        "Week",
        "Time",
        "Month",
        "Day",
        "Days",
        "Hours",
        "Today",
        "Night",
        "Past",
        "Long",
    };

    public static final String actionTerms[]  = {
        "Walk",
        "Run",
        "Steps",
        "Sleep",
        "Spend",
        "Calorie",
        "Calories",
        "Bill",
        "Money",
        "Eat",
        "Food",
        "Phone",
        "Called",
        "Miles",
        "Heart",
        "Work",
        "Weight",
        "Games",
        "Playing",
        "Active",
        "Burn",
        "Gym",
        "Period"
    };

    public static final String quantativeTerms[]  = {
        "Average",
        "Miles",
        "Amount",
        "Next",
        "More",
        "Often",
        "Daily"
    };
}
