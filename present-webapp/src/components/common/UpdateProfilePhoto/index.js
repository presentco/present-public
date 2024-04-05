import React, { Component } from 'react';
import {connect} from 'react-redux';
import ApiHandler from '../../../core/ApiHandler';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../redux/actions/userActions';
import * as commentActions from '../../../redux/actions/commentActions';
import PhotoCropper from '../../common/PhotoCropper';
import _ from 'lodash';
import Modal from '../../CircleHome/components/Modal';
import Amplitude from 'react-amplitude';
import amplitudeEvents from '../../../core/AmplitudeEvents';
import Spinner from 'halogen/ScaleLoader';

class UpdateProfilePhoto extends Component {

  constructor(props) {
    super(props);
    this.state = {}
    this.onChangeProfileImage = this.onChangeProfileImage.bind(this);
  }

  onChangeProfileImage(e){
    e.preventDefault();
    Amplitude.event(amplitudeEvents["SIGNUP_SET_PHOTO"]);
    var self = this,
        reader = new FileReader(),
        file = e.target.files[0];

    reader.onload = function(upload) {
      self.setState({
        image: upload.target.result,
        showCropModal:true,
      });
    };
    reader.readAsDataURL(file);
  }

  getCroppedCanvas(cropResult){
    this.setState({cropResult, showCropModal: false, spinnerVisible:true});

    let request = ApiHandler.attachHeader({
          uuid: ApiHandler.guid(),
          type: 'JPEG',
          content:cropResult.split(',')[1]
        }),
        self = this;

    self.props.actions.uploadNewfile(request).then(response => {
      let newrequest = ApiHandler.attachHeader({photoRef:{uuid: response.uuid, type:response.contentType }});
      self.props.actions.putUserPhoto(newrequest).then( response => {
        //we can add messaging later for suuccessful upload-- need design?
        this.setState({spinnerVisible:false});
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
  }

  renderCropModalView(){
    return(
      <Modal show={this.state.showCropModal}
             noPadding={true}
             width={true}
             closeModal={() => this.setState({showCropModal: false})}>
             <div>
               <PhotoCropper
                 cropPhoto={this.state.image}
                 cancelCrop={() => this.setState({showCropModal: false})}
                 getCroppedCanvas={this.getCroppedCanvas.bind(this)}
                 fromComponent={'editProfile'}
                 />
             </div>
      </Modal>
    )
  }

  renderSpinnerView(){
    return(
      <Modal show={this.state.spinnerVisible}
             noPadding={true}
             width={true}
             widthTrue={true}
             noBoxShadow={true}
             closeModal={() => this.setState({spinnerVisible: false})}>
             <div>
               <Spinner color="#8136ec" size="70px"/>
             </div>
      </Modal>
    )
  }

  render() {
    let currentUser = this.props.currentUser,
        showSpinner = <span/>;

    return(
      <div className="photo-upload-container">
      {this.state.showCropModal ? this.renderCropModalView() : <span/>}
      {this.state.spinnerVisible ? this.renderSpinnerView() : <span/>}
        <img src={this.state.cropResult ? this.state.cropResult : currentUser.photo ? currentUser.photo : require('../../../assets/images/profile-placeholder@2x.png')}
             className="ur-photo"
             alt=""/>
        <div className={this.props.fromComponent==="edit-profile" ? "cam-container more-camera-option" : "cam-container"}>
          <label for="file-upload" >
              <img className="cam-icon" src={require('../../../assets/images/fill-149.svg')} alt="upload"/>
               <input
                 id="file-upload"
                 type="file"
                 name="file"
                 onChange={this.onChangeProfileImage}
                 accept="image/jpeg"/>
          </label>
        </div>
      </div>
    )
  }
}

function mapStateToProps(state, ownProps) {
    return {
        currentUser: state.user.currentUser,
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...commentActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(UpdateProfilePhoto);
