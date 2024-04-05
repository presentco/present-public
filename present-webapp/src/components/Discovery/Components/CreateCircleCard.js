import React, {Component} from 'react';
import _ from 'lodash';

export class CreateCircleCard extends Component {

  constructor(props){
    super(props);

    this.state={}
  }


    render() {
      return (
          <div className="card-view" onClick={() => this.props.onClickCreateCircle(this.props.title)}>
            <button className="crad-btn">
              <div className="card-container">
                <div className="card-main-bg">
                  <div className="aspect-ratio">
                    <img alt={this.props.title} src={this.props.addPhoto} className="cover-photo"/>
                  </div>
                </div>
                <div className="info-container cir">
                  <div className="an-container">
                    <div className="location-cont location-cont-tit">
                      <span className="circle-title">
                        Create A Circle
                      </span>
                    </div>
                    <span className="here-text">Here</span>
                  </div>
                </div>
              </div>
            </button>
          </div>
      );
      }
}

export default CreateCircleCard;
