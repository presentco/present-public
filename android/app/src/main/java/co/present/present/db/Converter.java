package co.present.present.db;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import co.present.present.model.Interest;
import co.present.present.model.InterestKt;

public class Converter {

    private static Gson gson = new Gson();

    @TypeConverter
    public static List<String> fromString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Interest> fromInterestString(String value) {
        List<Interest> interests = new ArrayList<>();
        for (String string : Converter.fromString(value)) {
            interests.add(InterestKt.toInterest(string));
        }
        return interests;
    }

    @TypeConverter
    public static String fromInterestList(List<Interest> interests) {
        List<String> strings = new ArrayList<>();
        for (Interest interest : interests) {
            strings.add(interest.getCanonicalString());
        }
        return Converter.fromList(strings);
    }

}
