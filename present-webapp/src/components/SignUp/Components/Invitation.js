import React, { Component} from 'react';
import './style.css';
import validationUtiles from '../../../core/validationUtiles';
import amplitudeEvents from '../../../core/AmplitudeEvents';
import Constants from '../../../core/Constants'
import Amplitude from 'react-amplitude';

class Invitation extends Component {
  constructor(props){
    super(props);

    this.state={
      user:{
        first: props.currentUser.name.first ? props.currentUser.name.first : '',
        last: props.currentUser.name.last ? props.currentUser.name.last : '',
        email:props.currentUser.email ? props.currentUser.email : ''
      },
      errors:{},
      thankYou:false
    }

    this.onChangeInput = this.onChangeInput.bind(this);
    this.onClickRequest = this.onClickRequest.bind(this);
  }

  onChangeInput(e){
    e.preventDefault();
    let user = this.state.user,
        errors = this.state.errors;
        user[e.target.name] = e.target.value;
        errors[e.target.name] = "";
        this.setState({user,errors});
  }

  commonValidate(){
    let errMsg = validationUtiles.commonErrorValidation(this.state.user, this.state.errors);
    this.setState({errors:errMsg});
    return errMsg;
  }

  onClickRequest(){
    if(validationUtiles.errorEmpty(this.commonValidate())){
      this.props.onClickRequest(this.state.user);
      this.setState({thankYou:true});
      localStorage.clear();
    }
  }

  onclickSocialMedia(social){
    if(social === "fg"){
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

    if(!this.state.thankYou){
      return(
        <div>
          <div className="hide-for-small-only main-form-container">
              <div className="form-container">
                <p className="form-title">
                  Hey {this.props.currentUser.name ? this.props.currentUser.name.first : 'there'}, Present is invite only, get invited.
                </p>
                  <input
                    name="first"
                    value={this.state.user.first}
                    type="text"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                    first name
                  </label>
                  {this.state.errors.first ?
                  <span className="error-text">{this.state.errors.first}</span> :
                  <div/>
                  }
                  <input
                    name="last"
                    value={this.state.user.last}
                    type="text"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                      last name
                  </label>
                  {this.state.errors.last ?
                  <span className="error-text">{this.state.errors.last}</span> :
                  <div/>
                  }
                  <input
                    name="email"
                    value={this.state.user.email}
                    type="email"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                      email
                  </label>
                  {this.state.errors.email ?
                  <span className="error-text">{this.state.errors.email}</span> :
                  <div/>
                  }
                <div className="text-center">
                  <button onClick={this.onClickRequest} className="request-btn">Request an Invite</button>
                </div>
              </div>
          </div>
          <div className="show-for-small-only small-12 columns main-form-container">
              <div className="form-container">
                <p className="form-title">
                  Hey {this.props.currentUser.name ? this.props.currentUser.name.first : 'there'}, Present is invite only, get invited.
                </p>
                  <input
                    name="first"
                    value={this.state.user.first}
                    type="text"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                    first name
                  </label>
                  {this.state.errors.first ?
                  <span className="error-text">{this.state.errors.first}</span> :
                  <div/>
                  }
                  <input
                    name="last"
                    value={this.state.user.last}
                    type="text"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                      last name
                  </label>
                  {this.state.errors.last ?
                  <span className="error-text">{this.state.errors.last}</span> :
                  <div/>
                  }
                  <input
                    name="email"
                    value={this.state.user.email}
                    type="email"
                    onChange={this.onChangeInput}
                    className="input-form" />
                  <label className="field-label">
                      email
                  </label>
                  {this.state.errors.email ?
                  <span className="error-text">{this.state.errors.email}</span> :
                  <div/>
                  }

                <div className="text-center">
                  <button onClick={this.onClickRequest} className="request-btn">Request an Invite</button>
                </div>
              </div>
          </div>
        </div>

        )
    } else {
      return (
        <div className="small-12 medium-5 medium-centered large-5 large-centered columns main-form-container">
          <div className="form-container">
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
}

export default Invitation;
