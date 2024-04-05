
const facebookSDKLoader = (d, appId, fn, err) => {

  const id = 'fb-client';
  const fjs = d.getElementsByTagName('script')[0];
  let js;

  if (d.getElementById(id)) {
    return;
  }

  js = d.createElement('script');

  js.id = id;
  js.src = '//connect.facebook.net/en_US/all.js';

  js.onload = () => {
    window.fbAsyncInit = function () {
      window.FB.init({
        appId      : appId,
        status     : true,  // check login status
        cookie     : true,  // enable cookies to allow the server to access the session
        xfbml      : true,  // parse XFBML
        version    : "v2.8"
      });
    }
  }

  fjs.parentNode.insertBefore(js, fjs);
}

export default facebookSDKLoader