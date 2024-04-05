package present.web;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import java.util.regex.Pattern;

import static com.googlecode.objectify.ObjectifyService.register;
import static present.web.Servlets.get;

/**
 * @author Bob Lee (bob@present.co)
 */
public class AppEngineWebModule extends ServletModule {

  @Override protected void configureServlets() {
    // Objectify
    filter("/*").through(ObjectifyFilter.class);
    bind(ObjectifyFilter.class).in(Singleton.class);

    mapRedirects();

    // Redirect app referral links to "/".
    serve("/a/*").with(get((request, response) -> {
      // Redirect instead of forward so relative links work.
      response.sendRedirect("/app");
    }));

    // Web app
    serve("/app", "/app/createCircle", "/g/*", "/u/*", "/t/*", "/v/*")
        .with(WebAppServlet.class);

    // Mobile web app
    serve("/m", "/m/g/*", "/m/u/*", "/m/t/*", "/m/v/*", "/m/createCircle")
        .with(MobileAppServlet.class);

    // Jersey servlet
    serve("/rest/*").with(GuiceContainer.class);

    serve("/smsGroup", "/sms").with(SmsServlet.class);

    // Jersey resources
    bind(RequestInvitation.class);

    // Register entity types
    register(InvitationRequest.class);
  }

  private void mapRedirects() {
    redirect("dearmom").to("/dearmom.html");
    redirect("timesup").to("https://present.threadless.com/");
    redirect("joins").to("https://blog.present.co/coins-for-joins-ccc0a7191f40");
    redirect("galentines").to("https://www.eventbrite.com/e/present-galentines-day-tickets-42770860796");
    redirect("power").to("https://medium.com/@letsbepresent/present-partners-with-the-power-rising-summit-2018-6a5d93c0c30a");
    redirect("whyfacebook").to("https://medium.com/@letsbepresent/why-we-verify-with-facebook-da7d08f95c33");
    redirect("enough").to("https://blog.present.co/how-to-support-gun-policy-change-in-america-44a6d37af20c");
    redirect("sxsw").to("https://present.co/g/Mqj4qTgE");
    redirect("support").to("https://blog.present.co/present-support-3d4c39b7b175");
    redirect("apply").to("https://goo.gl/forms/Uz5oBY6kY1YPy3I53");
    redirect("share").to("https://medium.com/@letsbepresent/how-to-build-your-good-vibe-tribe-deea7283664d");
    redirect("circleup").to("https://blog.present.co/circle-up-ef7b8f1d2412");
    redirect("leanproduct").to("https://present.co/g/VwobHxdr");

    // Shortcuts for installs from social
    redirect("ig").to("/app?ref=ig");
    redirect("fb").to("/app?ref=fb");
    redirect("tw").to("/app?ref=tw");

    // Team shortcuts
    redirect("janete").to("https://present.co/u/RaDBhkkd");
    redirect("kassia").to("https://present.co/u/3QAGHWWk");
    redirect("kristina").to("https://present.co/u/lzLLxfg1");
    redirect("pegah").to("https://present.co/u/MPqkHxxb");
    redirect("lisa").to("https://present.co/u/MmOrueem");
    redirect("gabrielle").to("https://present.co/u/M6qdUppZ");
    redirect("bob").to("https://present.co/u/3Aagi723");
  }

  private static Pattern namePattern = Pattern.compile("[a-z0-9]*", Pattern.CASE_INSENSITIVE);

  /** Redirects "/name" to the given URL. */
  private Redirector redirect(String... names) {
    for (String name : names) {
      Preconditions.checkArgument(namePattern.matcher(name).matches(),
          "Supports letters and numbers only.");
    }
    return (to) -> {
      for (String name : names) {
        // Case insensitive regexp.
        serveRegex("(?i)\\/" + name).with(get((request, response) -> {
          // Redirect instead of forward so relative links work.
          response.sendRedirect(to);
        }));
      }
    };
  }

  interface Redirector {
    void to(String path);
  }
}
