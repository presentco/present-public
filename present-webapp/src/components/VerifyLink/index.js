import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as userActions from '../../redux/actions/userActions';
import ApiHandler from '../../core/ApiHandler';
import './style.css';

class VerifyLink extends Component {
  constructor(props) {
    super(props);

    this.state ={
      user:{}
    }

    this.goToMagicPage = this.goToMagicPage.bind(this);
  }

  componentDidMount(){
    localStorage.setItem('clientUuid', ApiHandler.guid());
  }


  goToMagicPage(){

    let url = window.location.href;

    if(window.location.href.indexOf('local') > -1){
      let newUrl = window.location.href.split('3000')[1];
      url = `https://staging.present.co${newUrl}`
    }

    let request = ApiHandler.attachHeader({url});
    this.props.actions.verifyUser(request).then(response => {
      
       this.context.router.push(`/app`);
    }).catch(error => {

      if(error.response){
          this.setState({showError: error.response.data.error.message});
      } else {
          this.setState({showError: error.message});
      }

      console.log(url);
    });
  }

  render() {
    return (
      <div className="body-verify-white">
        <img src="https://present.co/images/divider@2x.png" className="divider" alt="logo"/>
        <div className="row text-center">
          <div>
            <img src={require("../../assets/images/vertical-logo.svg")} className="logo-vertical"  alt="logo"/>
            <p className="title-network">Network of Extraordinary People</p>
            <a className="sign-in-btn" onClick={this.goToMagicPage}>Sign in to Present</a>
          </div>
          {this.state.showError ?
            <div className="text-center"><p className="error-msg">{this.state.showError}</p></div>
          : <span/>}
          <div className="app-container">
            <p>Don&apos;t have the app?</p>
            <a href="https://appsto.re/us/0lr-jb.i"  className="ios">
              <img alt="apple" src="https://present.co/images/download-on-the-app-store-badge-us-uk-135-x-40@2x.png"/>
            </a>
            <a href="https://play.google.com/store/apps/details?id=co.present.present" target="_blank" className="android">
              <img alt="play store" src="https://present.co/images/google-play@2x.png"/>
            </a>
          </div>
          <div className="social-media">
            <a href="https://www.instagram.com/letsbepresent/" target="_blank">
              <img src="https://present.co/images/instagram-new@2x.png" alt="instagram" />
            </a>
            <a href="https://blog.present.co/">
              <img src="https://present.co/images/medium-new@2x.png" alt="medium" target="_blank" />
            </a>
            <a href="https://twitter.com/letsbepresent">
              <img src="https://present.co/images/twitter-new@2x.png" alt="twitter" target="_blank" />
              </a>
            <a href="https://www.facebook.com/letsbepresent/">
              <img src="https://present.co/images/facebook-new@2x.png" alt="facebook" target="_blank" />
              </a>
          </div>
        </div>
      </div>
    );
  }
}

VerifyLink.contextTypes = {
 router: PropTypes.object
}


function mapStateToProps(state, ownProps) {
  let verifyId = ownProps.params.verifyId;
  return {
    verifyId: verifyId,
    currentUser: state.user.currentUser
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(userActions, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps) (VerifyLink);
