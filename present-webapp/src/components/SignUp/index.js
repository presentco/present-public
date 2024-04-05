import React, { Component,PropTypes} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../redux/actions/userActions';
import * as groupActions from '../../redux/actions/groupActions';
import './style.css';
import Constants from '../../core/Constants';
import Invitation from './Components/Invitation';
import GeneralLogin from './Components/GeneralLogin';
import _ from 'lodash';
import axios from 'axios';

class SignUp extends Component {
  constructor(props){
    super(props);
    this.state={
      facebookVisible: !_.isEmpty(props.currentUser) ? false : true,
      visibleRSVP: false,
      user:{},
      currentUser:{},
      disabled: false,
      currentGroup:{},
      showMobilecomponent:false
    }

    this.handleSocialLogin= this.handleSocialLogin.bind(this);
    this.onClickRequest = this.onClickRequest.bind(this);

  }

  //Called any time the Props have Changed in the Redux Store
  componentWillReceiveProps(nextProps) {
      //Check if the Props for Services have in fact changed.
      if (this.props.currentUser !== nextProps.currentUser) {
          this.setState({currentUser: nextProps.currentUser });
      }

      if (this.props.currentGroup !== nextProps.currentGroup) {
          this.setState({currentGroup: nextProps.currentGroup });
      }
  }

  handleSocialLogin(response){
    console.log(response);
  }

  onClickRequest(data){
    //uncomment this after header is fixed
    let formData = data;
      let request = axios.create({
        baseURL: Constants.BASE_URL,
         headers: {
           "Access-Control-Allow-Origin": "http://local.present.co:3000/",
           "Access-Control-Allow-Headers": "Origin, X-Requested-With, Content-Type, Accept"
         }
      });
      // Send the form data.
      request.post('/rest/requestInvitation', formData).then(response => {

      }).catch(function(err){
        console.log(err);
      });
  }

  render() {

    let stepView = <div/>,
        facebookVisible = <div/>,
        showUploadPhotoModal = <div/>;

    if(this.state.visibleRSVP){
      stepView = (
        <Invitation
          blockText={this.state.blockText}
          currentUser={this.state.currentUser}
          onClickRequest={this.onClickRequest}/>
      )
    }

    if(this.state.facebookVisible){
      let type;
      if(window.location.href.indexOf('/app') > -1){
        type="app";
      } else if(window.location.href.indexOf('/g/') > -1){
        type="circle";
      } else if(window.location.href.indexOf('/t/') > -1){
        type="category";
      } else if(window.location.href.indexOf('/u/') > -1){
        type = "user";
      }

      facebookVisible = (
      <div>
        <div className="row">
            <GeneralLogin vanityId={this.props.vanityId} onCloseFacebook={this.onCloseFacebook} type={type}/>
          </div>
       </div>
      )
    }

    return(
      <div className="sign-up-container ">
        {showUploadPhotoModal}
        {this.state.facebookVisible ? facebookVisible : stepView}
      </div>
    )

  }
}

SignUp.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
  let vanityId= ownProps.params.vanityId;
    return {
        vanityId: vanityId,
        currentUser: state.user.currentUser,
        currentGroup: state.group.currentGroup
    }
}

function mapDispatchToProps(dispatch) {

    return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(SignUp);
