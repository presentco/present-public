package present.server.model.user;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author pat@present.co
 */
public class UserSearch {

  public static Set<Key<User>> search(String searchText) {
    String[] terms = searchText
        .trim()
        .replace(",", "")
        .split(" ");

    if (terms.length < 1) { return Collections.emptySet(); }

    // Kick off all queries (terms * searchable fields)
    List<Iterable<Key<User>>> foundUsersByTerm = new ArrayList<>();
    for (String term : terms) {
      foundUsersByTerm.add(searchTermPartial(term));
    }

    // Distinct users per term
    List<Set<Key<User>>> userSets = foundUsersByTerm.stream()
        .map(Sets::newHashSet).collect(Collectors.toList());

    // Intersect users per term
    Set<Key<User>> resultUsers = userSets.stream().reduce(
      userSets.stream().findFirst().get(), (s1, s2) -> {
        s1.retainAll(s2); return s1; }
      );

    return resultUsers;
  }

  // Match search term against any searchable field
  public static Iterable<Key<User>> searchTermPartial(String term) {
    Iterable<Key<User>> firstNameMatches = searchFieldPartial(User.Fields.firstName.name(), term);
    Iterable<Key<User>> lastNameMatches = searchFieldPartial(User.Fields.lastName.name(), term);
    return Iterables.concat(firstNameMatches, lastNameMatches);
  }

  private static List<Key<User>> searchFieldPartial(String fieldName, String term)
  {
    int perQueryLimit = 50;
    return ofy().load()
        .type(User.class)
        .filter(fieldName + " >= ", term)
        .filter(fieldName + " < ", term + Character.MAX_VALUE)
        .limit(perQueryLimit)
        .keys()
        .list();
  }

}
