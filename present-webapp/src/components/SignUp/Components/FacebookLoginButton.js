

import React, {Component, PropTypes} from 'react';
import loaders from './social-loaders';
import * as userActions from '../../../redux/actions/userActions';
import * as groupActions from '../../../redux/actions/groupActions';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import Spinner from 'halogen/ClipLoader';
import './style.css';
import ApiHandler from '../../../core/ApiHandler';
import Constants from '../../../core/Constants';
import amplitudeEvents from '../../../core/AmplitudeEvents';
import Amplitude from 'react-amplitude';

/*global FB*/
class FacebookLoginButton extends Component {

    constructor(props){

      super(props);
      this.state={
          visibleSpinner: false,
          userProfile:{}
      }

      this.onPressSocialBtn = this.onPressSocialBtn.bind(this);
      this.handleSocialLoginSuccess = this.handleSocialLoginSuccess.bind(this);
      this.handleSocialLoginFailure = this.handleSocialLoginFailure.bind(this);
    }

    // Load Appropriate Provider
    componentDidMount() {

      const d = document;
      const appId = Constants.Keys.Facebook.APP_ID;
      loaders.facebook(d, appId, this.handleSocialLoginSuccess, this.handleSocialLoginFailure);
    }

    componentWillUnmount() {
     clearTimeout(this.refs.myRef);
   }

   loadRequestedGroupFromBackend(){

   //0424D3CD-7DF1-4EC8-B6FA-CD79C60FB827
   let url = `${Constants.environment.production}g/${this.props.vanityId}`;
     let request = ApiHandler.attachHeader({url:url});
      this.props.actions.getGroupInfo(request).then( response => {
        localStorage.setItem('invitedCircle', JSON.stringify(response));
     }).catch(error => {
       const errorMessage = ApiHandler.getErrorMessage(error);
       console.log(errorMessage);
     });
}

    handleSocialLoginSuccess (response) {

      let userData = {
        accessToken: null
      };

      userData.accessToken = response.authResponse.accessToken;
      this.loginUser(userData);
       Amplitude.event(amplitudeEvents["SIGNUP_FACEBOOK_ACCESS_ALLOWED"]);
    }

    handleSocialLoginFailure (err) {

      console.log(err);
      Amplitude.event(amplitudeEvents["SIGNUP_FACEBOOK_ACCESS_DENIED"]);
    }

    synchronize(userProfile){
      let authResponse={},
          request = ApiHandler.attachHeader({});

      this.props.actions.synchronize(request).then(step => {

         authResponse = step;
         if(this.props.type !== 'user-profile'){
            this.props.populateDataFromFB(userProfile);

            if(this.props.vanityId){
            this.context.router.push(`/g/${this.props.vanityId}`);
           }
         } else {
            this.props.onCloseFacebook();
         }

      }).catch(error => {

         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
      return authResponse;
   }

    loginUser(fbUserData) {
        //first check status of user
        let request = ApiHandler.attachHeader(fbUserData),
            authResponse;

        this.props.actions.login(request).then(userProfile => {

           if(this.props.type !== 'user-profile'){
             if(this.props.vanityId){
               this.loadRequestedGroupFromBackend();
            }
            }
            authResponse = this.synchronize(userProfile);
          setTimeout(()=>{
            if(this.refs.myRef){
             this.setState({visibleSpinner:false});
            }
          },1000);

         }).catch(error => {

            Amplitude.event(amplitudeEvents["SIGNUP_FACEBOOK_ACCESS_ALLOWED"]);
            const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
         });
    }

    onPressSocialBtn(){
      Amplitude.event(amplitudeEvents["SIGNUP_CONTINUE_WITH_FACEBOOK"]);
      let that = this;
      window.FB.login( response => {
      //eslint-disable-line no-undef
        if (response.status === 'connected') {
          this.setState({visibleSpinner: true});
          if(this.props.type !== 'user-profile'){
             this.props.visibleSpinner(false);
          }

          that.handleSocialLoginSuccess(response);
        }
      }, {scope: 'public_profile,email,user_friends'});
    }

    render() {

        //spinner
        let showSpinner = <div />,
            showFBButton = (
                <div>
                    <button className={this.props.type !== 'user-profile' ? "fb-login-btn fb-small" : "fb-login-btn fb-small-user-profile"} disabled={this.props.disabled} onClick={this.onPressSocialBtn}>
                           <p className="no-link-text">{this.props.type === 'user-profile' ? "Link your Facebook" : "Continue with Facebook"}</p>
                     </button>

                        {/*!this.state.showReason ?
                           <a className="why-fb-btn" onClick={() => this.setState({showReason:true})}>
                           Why Facebook?
                           </a>
                           <span/>
                           :
                           <p className="fb-reason">
                              We use Facebook to confirm our members are real people who identify as women. We won’t share your information.
                           </p>
                        */}
                </div>
            );

        if(this.state.visibleSpinner){
            showSpinner = (
                <div className="margin-top-1" ref="myRef">
                    <Spinner color="#8136ec" size="70px" margin="20px"/>
                </div>
            );
            showFBButton = <div />;
        }

      return (
        <div>
          {showFBButton}
          {showSpinner}
        </div>
      )
    }
}

FacebookLoginButton.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {

  return {
      currentUser: state.user.currentUser
  }
}

function mapDispatchToProps(dispatch) {
  return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(FacebookLoginButton);
