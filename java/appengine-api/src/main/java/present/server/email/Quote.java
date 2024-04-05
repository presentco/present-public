package present.server.email;

import com.google.common.collect.ImmutableList;
import io.netty.util.internal.ThreadLocalRandom;
import java.util.List;
import static present.server.email.SummaryEmail.indexForToday;

/**
 * Inspirational quotes.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Quote {

  public static final List<Quote> QUOTES = ImmutableList.of(
      new Quote("The most common way people give up their power is by thinking they don’t have any.", "Alice Walker"),
      new Quote("I love to see a young girl go out and grab the world by the lapels. Life’s a bitch. You’ve got to go out and kick ass.", "Maya Angelou"),
      new Quote("Do not wait for leaders; do it alone, person to person. Be faithful in small things because it is in them that your strength lies.", "Mother Teresa"),
      new Quote("It is our choices, that show what we truly are, far more than our abilities.", "J.K. Rowling"),
      new Quote("I believe that the rights of women and girls is the unfinished business of the 21st century.", "Hillary Clinton"),
      new Quote("I had no idea that history was being made. I was just tired of giving up.", "Rosa Parks"),
      new Quote("If you want something said, ask a man; if you want something done, ask a woman.", "Margaret Thatcher"),
      new Quote("I'm tough, I'm ambitious, and I know exactly what I want. If that makes me a bitch, okay.", "Madonna"),
      new Quote("It took me quite a long time to develop a voice, and now that I have it, I am not going to be silent.", "Madeleine Albright"),
      new Quote("Feminism isn't about making women strong. Women are already strong. It's about changing the way the world perceives that strength.", "G.D. Anderson"),
      new Quote("In the future, there will be no female leaders. There will just be leaders.", "Sheryl Sandberg"),
      new Quote("Human rights are women's rights and women's rights are human rights, once and for all.", "Hillary Clinton"),
      new Quote("Remember no one can make you feel inferior without your consent.", "Eleanor Roosevelt"),
      new Quote("Build your own dreams, or someone else will hire you to build theirs.", "Farrah Gray"),
      new Quote("Nothing will work unless you do.", "Maya Angelou"),
      new Quote("I alone cannot change the world, but I can cast a stone across the water to create many ripples.", "Mother Teresa"),
      new Quote("Instead of looking at the past, I put myself ahead twenty years and try to look at what I need to do now in order to get there then.", "Diana Ross"),
      new Quote("The most effective way to do it, is to do it.", "Amelia Earhart"),
      new Quote("The secret of getting ahead is getting started.", "Sally Berger"),
      new Quote("If your actions create a legacy that inspires others to dream more, learn more, do more and become more, then, you are an excellent leader.", "Dolly Parton"),
      new Quote("Success is getting what you want. Happiness is wanting what you get.", "Ingrid Bergman"),
      new Quote("We’re here for a reason. I believe that reason is to throw little torches out to lead people through the dark.", "Whoopi Goldberg"),
      new Quote("Always be a first-rate version of yourself instead of a second-rate version of somebody else.", "Judy Garland"),
      new Quote("The truth will set you free, but first it will piss you off.", "Gloria Steinem"),
      new Quote("I think self-awareness is probably the most important thing toward being a champion.", "Billie Jean King"),
      new Quote("If I stop to kick every barking dog I am not going to get where I’m going.", "Jackie Joyner-Kersee"),
      new Quote("When the whole world is silent, even one voice becomes powerful.", "Malala Yousafzai"),
      new Quote("You are the one that possesses the keys to your being. You carry the passport to your own happiness.", "Diane von Furstenberg"),
      new Quote("I didn’t get there by wishing for it or hoping for it, but by working for it.", "Estée Lauder"),
      new Quote("Power’s not given to you. You have to take it.", "Beyoncé Knowles Carter"),
      new Quote("The difference between successful people and others is how long they spend time feeling sorry for themselves.", "Barbara Corcoran"),
      new Quote("You can waste your lives drawing lines. Or you can live your life crossing them.", "Shonda Rhimes"),
      new Quote("I’d rather regret the things I’ve done than regret the things I haven’t done.", "Lucille Ball"),
      new Quote("You can never leave footprints that last if you are always walking on tiptoe.", "Leymah Gbowee"),
      new Quote("If you don’t like the road you’re walking, start paving another one.", "Dolly Parton"),
      new Quote("Step out of the history that is holding you back. Step into the new story you are willing to create.", "Oprah Winfrey"),
      new Quote("What you do makes a difference, and you have to decide what kind of difference you want to make.", "Jane Goodall"),
      new Quote("I choose to make the rest of my life the best of my life.", "Louise Hay"),
      new Quote("Spread love everywhere you go. Let no one ever come to you without leaving happier.", "Mother Teresa"),
      new Quote("Take criticism seriously, but not personally. If there is truth or merit in the criticism, try to learn from it. Otherwise, let it roll right off you.", "Hillary Clinton"),
      new Quote("Learn from the mistakes of others. You can’t live long enough to make them all yourself.", "Eleanor Roosevelt"),
      new Quote("Doubt is a killer. You just have to know who you are and what you stand for.", "Jennifer Lopez"),
      new Quote("No one changes the world who isn’t obsessed.", "Billie Jean King"),
      new Quote("Hold your head and your standards high even as people or circumstances try to pull you down.", "Tory Johnson"),
      new Quote("Owning our story can be hard but not nearly as difficult as spending our lives running from it.", "Brene Brown"),
      new Quote("If you don’t get out of the box you’ve been raised in, you won’t understand how much bigger the world is.", "Angelina Jolie"),
      new Quote("Don’t look at your feet to see if you are doing it right. Just dance.", "Anne Lamott"),
      new Quote("There are two kinds of people, those who do the work and those who take the credit. Try to be in the first group; there is less competition there.", "Indira Gandhi"),
      new Quote("This journey has always been about reaching your own other shore no matter what it is, and that dream continues.", "Diana Nyad"),
      new Quote("We do not need magic to change the world, we carry all the power we need inside ourselves already: we have the power to imagine better.", "J.K. Rowling"),
      new Quote("I need to listen well so that I hear what is not said.", "Thuli Madonsela"),
      new Quote("I try to live in a little bit of my own joy and not let people steal it or take it.", "Hoda Kotb"),
      new Quote("It’s not the absence of fear, it’s overcoming it. Sometimes you’ve got to blast through and have faith.", "Emma Watson"),
      new Quote("I learned compassion from being discriminated against. Everything bad that’s ever happened to me has taught me compassion.", "Ellen DeGeneres"),
      new Quote("Many receive advice, only the wise profit from it.", "Harper Lee"),
      new Quote("No matter how senior you get in an organization, no matter how well you’re perceived to be doing, your job is never done.", "Abigail Johnson"),
      new Quote("The mere fact of being able to call your job your passion is success in my eyes.", "Alicia Vikander"),
      new Quote("If you have an idea, you have to believe in yourself or no one else will.", "Sarah Michelle Gellar"),
      new Quote("The way you tell your story to yourself matters.", "Amy Cuddy"),
      new Quote("Buckle up, and know that it’s going to be a tremendous amount of work, but embrace it.", "Tory Burch"),
      new Quote("Run to the fire; don’t hide from it.", "Meg Whitman"),
      new Quote("I did not have the most experience in the industry or the most money, but I cared the most.", "Sara Blakely"),
      new Quote("Getting past those labels, for me, pretty much really easy because I define myself.", "Serena Williams"),
      new Quote("Without leaps of imagination, or dreaming, we lose the excitement of possibilities. Dreaming, after all, is a form of planning.", "Gloria Steinem"),
      new Quote("If we march the long road to freedom in hatred, what we find at the end is not freedom but another prison.", "Aung San Suu Kyi"),
      new Quote("Every moment wasted looking back keeps us from moving forward.", "Hillary Clinton"),
      new Quote("If you obey all the rules, you miss all the fun.", "Katherine Hepburn"),
      new Quote("Differences can be a strength rather than a handicap.", "Condoleezza Rice"),
      new Quote("It’s better to light a candle than curse the darkness.", "Eleanor Roosevelt"),
      new Quote("Don’t mistake politeness for lack of strength.", "Sonya Sotomayer"),
      new Quote("I can’t think of any better representation of beauty than someone who is unafraid to be herself.", "Emma Stone"),
      new Quote("It’s possible to climb to the top without stomping on other people.", "Taylor Swift"),
      new Quote("Courage is like a muscle. We strengthen it by use.", "Ruth Gordo"),
      new Quote("You have what it takes to be a victorious, independent, fearless woman.", "Tyra Banks"),
      new Quote("I don’t care what you think about me. I don’t think about you at all.", "Coco Chanel"),
      new Quote("Success is getting what you want, happiness is wanting what you get.", "Ingrid Bergman"),
      new Quote("Power to me is the ability to make a change in a positive way.", "Victoria Justice"),
      new Quote("No matter what you look like or think you look like you’re special and loved and perfect just the way you are.", "Ariel Winter"),
      new Quote("And the trouble is, if you don’t risk anything, you risk more.", "Erica Jong"),
      new Quote("Don’t be the girl who fell. Be the girl who got back up.", "Jenette Stanley"),
      new Quote("Think like a queen. A queen is not afraid to fail. Failure is another stepping stone to greatness.", "Oprah"),
      new Quote("Women are the largest untapped reservoir of talent in the world.", "Hillary Clinton"),
      new Quote("Being a strong woman is important to me. But doing it all on my own is not.", "Reba McEntire"),
      new Quote("I don’t believe in luck. It’s persistence, hard work, and not forgetting your dream.", "Janet Jackson"),
      new Quote("Never be limited by other people’s limited imaginations.", "Mae Jemison"),
      new Quote("I avoid looking forward or backward, and try to keep looking upward.", "Charlotte Bronte"),
      new Quote("What makes you different or weird, that’s your strength.", "Meryl Streep"),
      new Quote("A boat is always safe in the harbor, but that’s not what boats are built for.", "Katie Couric"),
      new Quote("A woman with a voice is by definition a strong woman.", "Melinda Gates"),
      new Quote("Optimism is the faith that leads to achievement; nothing can be done without hope.", "Helen Keller"),
      new Quote("I believe ambition is not a dirty word. It’s just believing in yourself and your abilities.", "Reese Witherspoon"),
      new Quote("I’m fearless, I don’t complain. Even when horrible things happen to me, I go on.", "Sophia Vergara"),
      new Quote("I encourage women to step up. Don’t wait for someone to ask you.", "Reese Witherspoon"),
      new Quote("I don’t do regret.", "Reese Witherspoon"),
      new Quote("A man’s got to do what a man’s got to do. A woman must do what he can’t.", "Rhonda Hansome"),
      new Quote("A strong woman looks a challenge in the eye and gives it a wink.", "Gina Carey"),
      new Quote("Girls should never be afraid to be smart.", "Emma Watson"),
      new Quote("I don’t like to gamble, but if there’s one thing I’m willing to bet on, it’s myself.", "Beyonce"),
      new Quote("It takes years as a woman to unlearn what you have been taught to be sorry for.", "Amy Poehler"),
      new Quote("Insecurity is a waste of time.", "Diane von Furstenberg")
  );

  public final String text;
  public final String author;

  public Quote(String text, String author) {
    this.text = text;
    this.author = author;
  }

  public static Quote random() {
    return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
  }

  public static Quote today() {
    return QUOTES.get(indexForToday(QUOTES.size()));
  }

  @Override public String toString() {
    return "“" + this.text + "” —" + this.author;
  }
}
