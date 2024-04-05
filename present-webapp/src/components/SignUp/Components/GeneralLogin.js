import React, { Component} from 'react';
import * as groupActions from '../../../redux/actions/groupActions';
import * as userActions from '../../../redux/actions/userActions';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import './style.css';
import _ from 'lodash';
import FacebookLoginButton from './FacebookLoginButton';
import EnterZipCode from './EnterZipCode';
import EnterPhoneNumber from './EnterPhoneNumber';
import EnterVerifyCode from './EnterVerifyCode';
import ConnectWithPhone from './ConnectWithPhone';
import BlockScreen from './BlockScreen';
import ApiHandler from '../../../core/ApiHandler';
import SliderView from './SliderView';
import amplitudeEvents from '../../../core/AmplitudeEvents';
import Amplitude from 'react-amplitude';
import UpdatePhoto from './UpdatePhoto';
import Spinner from 'halogen/ClipLoader';

class GeneralLogin extends Component {
  constructor(props){
    super(props);
    this.state={
      showDots:true,
      chosenPhoto:"2.png",
      loaded:false,
      urlInfo:{},
      phone: '',
      code: '',
      status:'phone',
      zip:'',
      user:{
        name:{
          first:'',
          last:''
        },
          photo:''
      }
    }

    this.visibleSpinner = this.visibleSpinner.bind(this);
    this.onCloseFacebook = this.onCloseFacebook.bind(this);
    this.onClickSendPhone = this.onClickSendPhone.bind(this);
    this.onChangeValue = this.onChangeValue.bind(this);
    this.onChangeUser = this.onChangeUser.bind(this);
    this.onClickThatsMe = this.onClickThatsMe.bind(this);
  }

  componentDidMount(){

    //Check if user already has a client uuid if not create a new one
    if(!localStorage.getItem('clientUuid')){
      Amplitude.event(amplitudeEvents["SIGNUP_VIEW_INTRO"]);
      localStorage.setItem('clientUuid', ApiHandler.guid());
    }

    this.setState({change:true});
    let request = ApiHandler.attachHeader({url: window.location.href});
    this.props.actions.getGroupInfo(request).then(respone => {
      this.setState({urlInfo:respone});
    }).catch(error => {
       const errorMessage = ApiHandler.getErrorMessage(error);
       console.log(errorMessage);
     });
  }


  onCloseFacebook(res){
    this.props.onCloseFacebook(res);
  }


    onClickSendPhone(){
      if(this.state.status === 'phone'){
        this.setState({status: 'code'});
        this.requestVerification();
      } else {
        this.verifyCode();
      }
    }

    requestVerification(val){
      let request = ApiHandler.attachHeader({phoneNumber: `1${this.state.phone}`});
      this.props.actions.verifyPhoneNumber(request).then(response => {

        this.setState({status: 'code', matchedCode:response.codeLength});
      }).catch(error => {
        throw(error);
      });
    }

    onClickThatsMe(){

      this.setState({visibleSpinner:true});

      let user = this.state.fromFacebook ? {name:{first:this.state.user.name.first, last: this.state.user.name.last}} : this.state.user;
      user.zip = this.state.zip;

      let request = ApiHandler.attachHeader({});
          request.argument = user
      this.props.actions.putUserProfile(request).then(response => {

        this.completeSignUp(user);
      }).catch(error => {

        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });
    }

    completeSignUp(){
      let request = ApiHandler.attachHeader({});

      this.props.actions.completeSignUp(request).then(res => {

        if(res.nextStep === "BLOCK"){
          Amplitude.event(amplitudeEvents["SIGNUP_BLOCKED"]);
          this.setState({visibleSpinner:false, status: 'blocked', blockText: res.blockScreen.text});
          localStorage.clear();
        } else if(res.nextStep === "PROCEED"){
          if(localStorage.getItem('authResponse') !== 'PROCEED'){
            localStorage.setItem('authResponse', 'PROCEED');
          }
          Amplitude.event(amplitudeEvents["SIGNUP_COMPLETED"]);
          window.location.reload();
          if(this.props.vanityId){
            this.context.router.replace(`/g/${this.props.vanityId}`);
          }
        }
      }).catch(error => {
           const errorMessage = ApiHandler.getErrorMessage(error);
           console.log(errorMessage);
        });
    }


    onChangeUser(e){
      let user= this.state.user;
      user.name[e.target.name] = e.target.value;

      this.setState({user});
    }

    verifyCode(){
      let request=ApiHandler.attachHeader({code: this.state.code}),
          user=this.state.user;

         this.setState({visibleSpinner:true});
      this.props.actions.verifyCode(request).then(response => {

        this.setState({visibleSpinner:false});

        if(response.data && response.data.error.message === 'Invalid verification code.'){
          this.setState({showError:true})
        } else {
          //if it is Proceed you dont need to complete sign up u save the user and send it to dicovery
          if(response.authorization.nextStep === "PROCEED"){
            localStorage.setItem('authResponse', 'PROCEED');
              if(!localStorage.getItem('currentUser')){
                localStorage.setItem('currentUser', JSON.stringify(response.userProfile));
              }

              Amplitude.event(amplitudeEvents["SIGNUP_COMPLETED"]);
              window.location.reload();

            //IF IT IS SIGN UP POPULATE THE USER INFO FOR COMPLETE SIGN UP
          } else if(response.authorization.nextStep === 'SIGN_UP'){
            if(response.userProfile){
              user = response.userProfile;
              this.setState({status:'confirmed', user});
            }
            //if block send user to block screen
          } else if(response.authorization.nextStep === 'BLOCK'){
            this.setState({status: 'blocked', blockText: response.authorization.blockScreen.text});
            localStorage.clear();
          }
        }
      }).catch(error => {

        if(error){
          this.setState({showError:true})
        }
      });
    }


    onEnterResend(){
      this.requestVerification();
    }

  onChangeValue(e){
    let newInput = e.target.value;
      newInput = newInput.replace("(", "");
      newInput = newInput.replace(")", "");
      newInput = newInput.replace("-", "");
      newInput = newInput.replace(" ", "");

    if(this.state.status === 'phone'){
      this.setState({phone: newInput});
    } else{
      if(newInput.length <= this.state.matchedCode){
          this.setState({code: newInput});
      }

    }
  }

  formatPhone(phone){
    let newPhone = ""
    for (let i = 0; i < phone.length; i++) {
      newPhone += phone[i];
      if(i === 2 || i === 5){
        newPhone += '-';
      }
    }
    return newPhone;
  }


  visibleSpinner(res){
    this.setState({showDots: res})
  }


  render() {
    let showSpinner=<span/>;

    if(this.state.visibleSpinner){
        showSpinner=(
            <div className="margin-top-1 text-center" ref="myRef">
                <Spinner color="#8136ec" size="70px" margin="20px"/>
            </div>
        );
    }

      let title = this.props.type === "app" ? "JOIN PRESENT TO" : "YOU'VE BEEN INVITED TO";

      return(
        <div>

        <div className="row no-link-container show-for-medium-up">
        <img src={require('../../../assets/images/background@2x.png')} className="login-bg " alt="bg"/>
        {!this.state.connectWithPhone ?
          <div className="small-12 small-centered medium-12 medium-centered large-10 large-centered position-relative columns">
            <div className="login-logo-container">
              <img alt="logo" src={require('../../../assets/images/login-logo.svg')} className="first-logo" />
              <p className="join-to-text">{title}</p>
            </div>
                <SliderView type={this.props.type}
                  urlInfo={this.state.urlInfo}
                  onClickCard={() => this.setState({connectWithPhone :true})}/>
                <div className={this.props.type === "app" ? "sign-container" : this.props.type === "circle" || this.props.type === "user" ? "sign-container fb-btn-wierd" : "sign-container fb-btn-wierd-cate"}>
                    <ConnectWithPhone onClickConnect={() => this.setState({connectWithPhone :true})}/>

                    <p className="terms-text">
                      By continuing you agree to our
                       <a onClick={() => Amplitude.event(amplitudeEvents["SIGNUP_VIEW_TOS"])}
                         className="terms-links"
                         target="_blank"
                         href="http://www.present.co/tos.html">
                          terms & privacy policy</a>
                    </p>

              </div>
          </div> :
          <div className="row" >
            <div className="medium-6 large-6 columns">
              <div className="white-logo-container">
                <img className="white-logo-present" src={require('../../../assets/images/white-present-logo@2x.png')} alt="present"/>
                <p className="phone-first-title">WELCOME TO PRESENT</p>
                <p className="phone-second-title">{this.state.status === 'linkFacebook' ? 'Enter your name and add a photo' : 'Enter your information to continue'}</p>
              </div>
            </div>

            <div className="medium-6 large-6 columns">
              <div className={this.state.status !== 'linkFacebook' ? this.state.status === 'confirmed' ? "verify-number-container less-padding-left" : this.state.status === 'blocked' ? 'verify-number-container less-width-container': "verify-number-container" : !this.state.hideFBbutton ? "verify-number-container verify-with-facebook" : "verify-number-container less-padding-verify"}>

                {this.state.status === 'phone' ?
                  <EnterPhoneNumber
                    sendPhone={(phone) => this.setState({phone})}/> :
                  <span/>}


                  {this.state.status === 'code' ?
                    <EnterVerifyCode
                      sendCode={(code) => this.setState({code})}
                      matchedCode={this.state.matchedCode}
                      phone={this.state.phone}
                      showError={this.state.showError}
                      goback={() => this.setState({status:'phone'})}/>
                    :
                    <span/>}


                  {this.state.status === 'phone' || this.state.status === 'code' ?
                    <button className="fb-login-btn fb-small" onClick={this.onClickSendPhone}>
                      <p className="no-link-text">{this.state.status === 'phone' ? 'Send Verification' : 'Next'}</p>
                    </button> :
                    <span/>}

                    {this.state.status === 'code' ?
                      <p className="or-connect-fb cursor-poiner" onClick={this.onEnterResend.bind(this)}>Resend Code</p>
                    : <span/>}


                  {this.state.status === 'confirmed' ?
                    <EnterZipCode
                      currentUser={this.state.user}
                      onClickThatsMe={(zip) => this.setState({zip, status: 'linkFacebook'})}
                      /> :
                    <span/>}


                  {this.state.status === 'linkFacebook' ?
                    <div>
                      {!this.state.hideFBbutton ?
                        <div>
                          <FacebookLoginButton
                            vanityId={this.props.vanityId}
                            onCloseFacebook={this.onCloseFacebook}
                            visibleSpinner={this.visibleSpinner}
                            populateDataFromFB={(user) => this.setState({user, hideFBbutton: true, fromFacebook:true})} />
                          <p className="or-devider">or</p>
                        </div>
                      : <span/>}
                      <UpdatePhoto user={this.state.user}/>
                      <input
                        name="first" value={this.state.user.name.first}
                        className="first-name-input"
                        onChange={this.onChangeUser}
                        placeholder="First"/>
                      <input
                        name="last"
                        value={this.state.user.name.last}
                        className="first-name-input"
                        onChange={this.onChangeUser}
                        placeholder="Last"/>

                    <div className="text-center">
                      <button
                        onClick={this.onClickThatsMe}
                        className={this.state.user.name.first !== '' && this.state.user.name.last !== '' ? 'thats-me-phone-btn' : "thats-me-phone-btn disabled"}>
                        That's Me!
                      </button>
                    </div>
                  </div> :
                  <span/>}


                {this.state.status === 'blocked' ?
                  <BlockScreen blockText={this.state.blockText}/>
                :
              <span/>}

                {showSpinner}
              </div>
            </div>
          </div>
        }
        </div>

        <div className="row no-link-container show-for-small-down scrollable">

            <div className="small-11 small-centered columns">
              <div className="text-center">
                <img alt="logo" src={require('../../../assets/images/login-logo.svg')} className="first-logo display-inline-important" />
                {!this.state.connectWithPhone ?
                  <p className="join-to-text text-center">{title}</p> :
                    <div>
                      <p className="phone-first-title black-color">WELCOME TO PRESENT</p>
                      <p className="phone-second-title black-color">{this.state.status === 'confirmed' ? 'Enter your name and add a photo' : 'Enter your information to continue'}</p>
                    </div>
                  }
              </div>
              {!this.state.connectWithPhone ?
                <div>
                  <SliderView type={this.props.type} urlInfo={this.state.urlInfo}/>
                  <div className="small-11 small-centered columns">
                    <div className="sign-container-small text-center">
                        <ConnectWithPhone onClickConnect={() => this.setState({connectWithPhone :true})}/>

                        <p className="terms-text">
                          By continuing you agree to our
                           <a onClick={() => Amplitude.event(amplitudeEvents["SIGNUP_VIEW_TOS"])}
                             className="terms-links"
                             target="_blank"
                             href="http://www.present.co/tos.html">
                              terms & privacy policy</a>
                        </p>

                    </div>
                  </div>
                </div>

              :
              <div className="verify-number-container-mobile">
                {this.state.status === 'phone' ?
                  <EnterPhoneNumber sendPhone={(phone) => this.setState({phone})}/> :
                  <span/>}

                  {this.state.status === 'code' ?
                  <EnterVerifyCode
                    sendCode={(code) => this.setState({code})}
                    matchedCode={this.state.matchedCode}
                    phone={this.state.phone}
                    showError={this.state.showError}
                    goback={() => this.setState({status:'phone'})}/> :
                  <span/>}
                  {this.state.status === 'confirmed' ?
                    <EnterZipCode
                      type="mobile-view"
                      currentUser={this.state.user}
                      onClickThatsMe={(zip) => this.setState({zip, status: 'linkFacebook'})}/> :
                    <span/>}

                  {this.state.status === 'linkFacebook' ?
                    <div>
                    {!this.state.hideFBbutton ?
                      <div>
                        <FacebookLoginButton
                          vanityId={this.props.vanityId}
                          onCloseFacebook={this.onCloseFacebook}
                          visibleSpinner={this.visibleSpinner}
                          populateDataFromFB={(user) => this.setState({user, hideFBbutton: true, fromFacebook:true})}/>
                        <p className="or-devider">or</p>
                      </div>
                    : <span/>}
                    <div>
                      <UpdatePhoto user={this.state.user}/>
                      <input
                        name="first" value={this.state.user.name.first}
                        className="first-name-input"
                        onChange={this.onChangeUser}
                        placeholder="First"/>
                      <input
                        name="last"
                        value={this.state.user.name.last}
                        className="first-name-input"
                        onChange={this.onChangeUser}
                        placeholder="Last"/>
                    </div>
                  <div className="text-center">
                    <button
                      onClick={this.onClickThatsMe}
                      className={this.state.user.name.first !== '' && this.state.user.name.last !== '' ? 'thats-me-phone-btn' : "thats-me-phone-btn disabled"}>
                      That's Me!
                    </button>
                  </div>
                </div> : <span/>}



              {this.state.status === 'phone' || this.state.status === 'code' ?
              <button className="fb-login-btn fb-small" onClick={this.onClickSendPhone}>
                <p className="no-link-text">{this.state.status === 'phone' ? 'Send Verification' : 'Next'}</p>
              </button> : <span/>}

            {this.state.showError && this.state.status !== 'confirmed' ?
              <p className="error-verify">This code is invalid. Please reenter or resend code</p> : <span/>}

                {showSpinner}

                {this.state.status === 'blocked' ?
                  <BlockScreen blockText={this.state.blockText}/>
                :
              <span/>}
              </div>
            }
            </div>

        </div>
        </div>

      )


  }
}

function mapStateToProps(state, ownProps) {

  return {}
}

function mapDispatchToProps(dispatch) {
  return {
      actions: bindActionCreators({...groupActions,...userActions}, dispatch)
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(GeneralLogin);
