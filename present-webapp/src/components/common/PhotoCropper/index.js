import React, {Component} from 'react';
import Cropper from 'react-cropper';

class PhotoCropper extends Component {

  constructor(props) {
    super(props);
    this.state = {
      cropResult:""
    }
    this.onClickCrop= this.onClickCrop.bind(this);

  }

    onClickCrop(){
      if (typeof this.cropper.getCroppedCanvas() === 'undefined') {
        //if we cant find the input for uploading photo
        return;
      }
      this.setState({
        cropResult: this.cropper.getCroppedCanvas().toDataURL()
      });

      this.props.getCroppedCanvas(this.cropper.getCroppedCanvas().toDataURL());
    }


  render() {

    return(
      <div>
        <Cropper
           ref={cropper => { this.cropper = cropper; }}
           style={{height: 400, width: '100%'}}
           src={this.props.cropPhoto}
           aspectRatio={this.props.fromComponent !== "editProfile" ? 16 / 9 : 1 / 1}
           guides={false}
         />
         <div className="crop-btn-opt">
           <button className="cancel-crop" onClick={() => this.props.cancelCrop()}>Cancel</button>
           <button className="cancel-crop" onClick={this.onClickCrop}>Choose</button>
         </div>
      </div>
    )
  }
}

export default PhotoCropper;
