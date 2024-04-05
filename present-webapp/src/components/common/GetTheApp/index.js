import React, {Component} from 'react';
import _ from 'lodash';
import Modal from '../../CircleHome/components/Modal';
import Constants from '../../../core/Constants';

class GetTheApp extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showDlBtn:false
    }
  }

  renderDownloadModal(){
    return(
      <Modal show={this.state.showDlBtn}
            width={true}
             closeModal={() => this.setState({showDlBtn: false})}>
             <div>
               <div className="dl-cta-container">
                 <img src={require('../../../assets/images/cta-get-app@2x.png')} alt="cta" className="dl-photo"/>
                 <div className="cta-container">
                   <p className="first-dl">Hey {this.props.currentUser.name.first},</p>
                   <p className="first-dl no-margin">Thanks for joining Present! Be sure to stay connected with our app :)</p>
                 </div>

               </div>
               <div className="dl-image-container">
                 <span className="choose-text">CHOOSE YOUR PLATFORM</span>
                 <p className="cta-text">Download the app</p>
                 <a onClick={() => this.setState({showDlBtn: false})} href={Constants.downloadLinks.appStore} target="_blank">
                   <img
                     className="dl-photo-phone"
                     src={require('../../../assets/images/download-on-the-app-store-badge-black@2x.png')}
                     alt="itunes"
                     onClick={() => window.location.reload()}/>
                 </a>
                 <a onClick={() => this.setState({showDlBtn: false})} href={Constants.downloadLinks.android} target="_blank">
                   <img
                     className="dl-photo-phone"
                     src={require('../../../assets/images/google-play@2x.png')}
                     alt="google-play"/>
                </a>
               </div>
             </div>
      </Modal>
    )
  }


  render() {

    return(
    <div>
      {this.state.showDlBtn ? this.renderDownloadModal() : <span/>}
      <div className="get-app-btn cursor-poiner">
        <a onClick={() => this.setState({showDlBtn:true})}>
          Get the App
        </a>
      </div>
    </div>
    )
  }
}

export default GetTheApp;
