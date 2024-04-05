package present.server.model.user;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import present.proto.ValidStateTransitionResponse;

/**
 * State of a user's account
 *
 * @author Bob Lee (bob@present.co)
 */
public enum UserState {

  INVITED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(PRE_APPROVED, REJECTED, SIGNING_UP);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return EnumSet.of(PRE_APPROVED, REJECTED);
    }

    @Override String description() {
      return "User was invited via SMS.";
    }

    @Override String color() {
      return "#eeeeff";
    }

    @Override public String adminVerb() {
      return "invite";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  SIGNING_UP {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(PRE_APPROVED, REJECTED, REVIEWING, MEMBER);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return EnumSet.of(PRE_APPROVED, REJECTED);
    }

    @Override String description() {
      return "User is signing up.";
    }

    @Override String color() {
      return "#eeeeff";
    }

    @Override public String adminVerb() {
      return "require-signup";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  PRE_APPROVED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(SIGNING_UP, MEMBER);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return EnumSet.of(SIGNING_UP);
    }

    @Override String description() {
      return "User is pre-approved.";
    }

    @Override String color() {
      return "#eeeeff";
    }

    @Override public String adminVerb() {
      return "pre-approve";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  REVIEWING {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(MEMBER, WAITING, REJECTED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "We need to review the user's application.";
    }

    @Override String color() {
      return "#eeeeee";
    }

    @Override public String adminVerb() {
      return "request review";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  WAITING {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(REVIEWING, REJECTED, MEMBER);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "User is on our waiting list.";
    }

    @Override String color() {
      return "#eeeeee";
    }

    @Override public String adminVerb() {
      return "wait-list";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  REJECTED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(REVIEWING, WAITING, MEMBER, DELETED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "User's application was rejected.";
    }

    @Override String color() {
      return "#ffeeee";
    }

    @Override public String adminVerb() {
      return "reject";
    }

    @Override public boolean preMembership() {
      return true;
    }
  },

  MEMBER {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(SUPPRESSED, SUSPENDED, GHOSTED, DELETED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "User is an active member.";
    }

    @Override String color() {
      return "#eeffee";
    }

    @Override public String adminVerb() {
      return "approve";
    }

    @Override public boolean preMembership() {
      return false;
    }
  },

  SUSPENDED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(MEMBER, SUPPRESSED, GHOSTED, DELETED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "Account is suspended. User can't log in. Content is still visible.";
    }

    @Override String color() {
      return "#ffeeee";
    }

    @Override public String adminVerb() {
      return "suspend";
    }

    @Override public boolean preMembership() {
      return false;
    }
  },

  SUPPRESSED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(MEMBER, SUSPENDED, GHOSTED, DELETED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "User is suppressed. User can't log in. Content is invisible.";
    }

    @Override String color() {
      return "#ffeeee";
    }

    @Override public String adminVerb() {
      return "suppress";
    }

    @Override public boolean preMembership() {
      return false;
    }
  },

  GHOSTED {
    @Override public Set<UserState> validTransitions() {
      return EnumSet.of(MEMBER, SUSPENDED, SUPPRESSED, DELETED);
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "User is ghosted. User can log in but their content is invisible.";
    }

    @Override String color() {
      return "#ffeeee";
    }

    @Override public String adminVerb() {
      return "ghost";
    }

    @Override public boolean preMembership() {
      return false;
    }
  },

  DELETED {
    @Override public Set<UserState> validTransitions() {
      return Collections.emptySet();
    }

    @Override public Set<UserState> validAdminTransitions() {
      return validTransitions();
    }

    @Override String description() {
      return "Account is pending deletion. User can't log in. Content is invisible.";
    }

    @Override String color() {
      return "#ffeeee";
    }

    @Override public String adminVerb() {
      return "delete";
    }

    @Override public boolean preMembership() {
      return false;
    }
  };

  static {
    for (UserState state : values()) {
      Preconditions.checkState(!state.validTransitions().contains(state));
      Preconditions.checkState(!state.validAdminTransitions().contains(state));
    }
  }

  /** Superset of valid transitions. */
  public abstract Set<UserState> validTransitions();

  /** Transitions administrators can make. */
  public abstract Set<UserState> validAdminTransitions();

  /** Verb describing the transition to this state. */
  public abstract String adminVerb();

  abstract String description();

  abstract String color();

  /**
   * If true, the user is in a state prior to membership. They have not been welcomed onto the
   * network yet, and they could not have created content.
   */
  public abstract boolean preMembership();

  public ValidStateTransitionResponse toResponse() {
    return new ValidStateTransitionResponse(name(), adminVerb(), description());
  }

  /** Generates a state graph. */
  public static void main(String[] args) throws IOException, InterruptedException {
    StringBuffer digraph = new StringBuffer("// Generated by ")
        .append(UserState.class.getName())
        .append("\n")
        .append("digraph {\n");
    for (UserState state : values()) {
      digraph.append(state.name())
          .append(" [style=filled, fillcolor=\"")
          .append(state.color())
          .append("\", xlabel=\"")
          .append(state.description())
          .append("\"]\n");
    }
    for (UserState state : values()) {
      for (UserState next : state.validTransitions()) {
        digraph.append(state.name())
            .append(" -> ")
            .append(next.name())
            .append("\n");
      }
    }
    digraph.append("}");

    System.out.println(digraph);

    String outputFile = "java/docs/user-states.png";
    Process p = new ProcessBuilder("dot", "-Tpng", "-o" + outputFile)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start();
    OutputStream out = p.getOutputStream();
    out.write(digraph.toString().getBytes(Charsets.UTF_8));
    out.close();
    p.waitFor();

    new ProcessBuilder("open", outputFile).inheritIO().start().waitFor();
  }
}
