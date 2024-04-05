
import React, { Component} from 'react';
import './style.css';
import amplitudeEvents from '../../../core/AmplitudeEvents';
import Constants from '../../../core/Constants';
import Amplitude from 'react-amplitude';

class BlockScreen extends Component {

  componentDidMount(){
    Amplitude.event(amplitudeEvents["SIGNUP_VIEW_WAIT_FOR_APPROVAL"]);
  }

  onclickSocialMedia(social){
    if(social === "fb"){
      window.location.replace('https://www.facebook.com/letsbepresent/')
    }

    if(social === "ig"){
      window.location.replace('https://www.instagram.com/letsbepresent/')
    }

    if(social === "tw"){
      window.location.replace('https://twitter.com/letsbepresent')
    }

    Amplitude.event(amplitudeEvents["SIGNUP_FOLLOW_SOCIAL_LINK"], {"Destination": Constants.AmplitudeProperties.social[social]});
  }

  render(){

      return (
        <div className="small-12 medium-12 large-12 columns main-form-container">
            <div className="">
            <p className="present-only">{this.props.blockText}</p>
            <div className="text-center social-container">
              <img
                src={require('../../../assets/images/shape-fb@2x.png')}
                alt="facebook"
                className="fb-social cursor-poiner"
                onClick={this.onclickSocialMedia.bind(this, "fb")}/>
              <img
                src={require('../../../assets/images/shape-instagram@2x.png')}
                alt="instagram"
                className="fb-social cursor-poiner"
                onClick={this.onclickSocialMedia.bind(this, "ig")}
                />
              <img
                src={require('../../../assets/images/shape-twitter@2x.png')}
                alt="twitter"
                className="fb-social cursor-poiner"
                onClick={this.onclickSocialMedia.bind(this, "tw")}
                />
            </div>
          </div>
        </div>
      )
  }
}

export default BlockScreen;
