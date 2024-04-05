import React, { Component } from 'react';
import ApiHandler from '../../../core/ApiHandler';
import PhotoCropper from '../../common/PhotoCropper';
import Api from '../../../core/Api';
import _ from 'lodash';
import Modal from '../../CircleHome/components/Modal';
import Spinner from 'halogen/ScaleLoader';

class UpdatePhoto extends Component {

  constructor(props) {
    super(props);
    this.state = {}
    this.onChangeProfileImage = this.onChangeProfileImage.bind(this);
  }

  onChangeProfileImage(e){
    e.preventDefault();
    var self = this;
    var reader = new FileReader();
    var file = e.target.files[0];
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
    });

    return Api.post('/ContentService/putContent', request).then(response => {

     let newrequest = ApiHandler.attachHeader({photoRef:{uuid: response.data.result.uuid, type:response.data.result.contentType }});
      return Api.post('/UserService/putUserPhoto', newrequest).then(response => {
        //we can add messaging later for suuccessful upload-- need design?
        this.setState({spinnerVisible:false});
        this.props.userHasPhoto();
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
  }


  render() {
    let user = this.props.user,
        showCropModal = <span/>,
        showSpinner = <span/>;

    if(this.state.showCropModal){
        showCropModal= (
          <Modal show={this.state.showCropModal}
                 closeModal={() => this.setState({showCropModal: false})}>
                 <div>
                   <PhotoCropper
                     cropPhoto={this.state.image}
                     cancelCrop={() => this.setState({showCropModal: false})}
                     getCroppedCanvas={this.getCroppedCanvas.bind(this)}
                     />
                 </div>
          </Modal>
      )
    }

    if(this.state.spinnerVisible){
      showSpinner=(
        <Modal show={this.state.spinnerVisible}
               closeModal={() => this.setState({spinnerVisible: false})}>
               <div>
                 <Spinner color="#8136ec" size="70px"/>
               </div>
        </Modal>
      )
    }

    return(

      <div className="photo-upload-container">
      {showCropModal}
      {showSpinner}
        <div className="cam-container-verify more-camera-option">
          <label for="file-upload" >
              <img
                className={user.photo ? "user-profile-verify" : "cam-icon-verify"}
                src={this.state.cropResult ? this.state.cropResult : user.photo ? user.photo : require('../../../assets/images/phone-veri-photo@2x.png')}
                alt=""/>
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

export default UpdatePhoto;
