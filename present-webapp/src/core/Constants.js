
const Constants = {

    COMPANY_NAME: "Present",
    BASE_URL: function() {
      // TODO: Ask the server for the API URL so we don't expose staging/dev URLs to users.
      // React dev server
      if (process.env.NODE_ENV !== 'production') return "https://api.staging.present.co/api";

      // App Engine dev server
      if (window.location.hostname.lastIndexOf('local', 0) === 0) return "http://local.present.co:8081/api";

      // Staging and production

       return window.location.protocol + "//api." + window.location.hostname + "/api";
    }(),

    environment: {
      staging: "http://api.staging.present.co/",
      production: "https://api.present.co/",
    },
    circleUrl:{
      url: process.env.NODE_ENV === 'production' ? "https://present.co/" : "http://staging.present.co/"
  },

    Keys: {
        Facebook: {
            APP_ID: "656395011206413"
        },
        Amplitude: {
          API_ID: process.env.NODE_ENV !== 'production' ? '2ee4651ca49b2dcd095be1d12c752bef' : '6a2ecfb250e9126666553f8b3022f2de'
        }
    },
    AmplitudeProperties: {
      setPhoto:{
          yes: "Yes",
          no: "No"
      },
      social:{
        fb: "Facebook",
        ig: "Instagram",
        tw: "Twitter"
      }
    },
    downloadLinks :{
      appStore: "https://appsto.re/us/0lr-jb.i",
      android: "https://play.google.com/store/apps/details?id=co.present.present"
    }
};

export default Constants;
