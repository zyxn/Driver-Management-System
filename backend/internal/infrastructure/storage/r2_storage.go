package storage

import (
	"context"
	"fmt"
	"io"
	"log"

	"driver-management-backend/internal/domain/repository"
	"driver-management-backend/internal/infrastructure/config"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

type r2StorageImpl struct {
	client     *s3.Client
	bucketName string
	publicURL  string
}

func NewR2Storage(cfg *config.Config) repository.StorageRepository {
	customResolver := aws.EndpointResolverWithOptionsFunc(func(service, region string, options ...interface{}) (aws.Endpoint, error) {
		return aws.Endpoint{
			URL:               cfg.R2.Endpoint,
			HostnameImmutable: true,
		}, nil
	})

	awsCfg, err := awsconfig.LoadDefaultConfig(context.TODO(),
		awsconfig.WithEndpointResolverWithOptions(customResolver),
		awsconfig.WithCredentialsProvider(credentials.NewStaticCredentialsProvider(cfg.R2.AccessKeyID, cfg.R2.SecretAccessKey, "")),
		awsconfig.WithRegion("auto"),
	)
	if err != nil {
		log.Fatalf("Failed to initialize AWS config for R2: %v", err)
	}

	client := s3.NewFromConfig(awsCfg)

	return &r2StorageImpl{
		client:     client,
		bucketName: cfg.R2.BucketName,
		publicURL:  cfg.R2.PublicURL,
	}
}

func (r *r2StorageImpl) UploadFile(ctx context.Context, fileName string, contentType string, file io.Reader) (string, error) {
	_, err := r.client.PutObject(ctx, &s3.PutObjectInput{
		Bucket:      aws.String(r.bucketName),
		Key:         aws.String(fileName),
		Body:        file,
		ContentType: aws.String(contentType),
	})

	if err != nil {
		return "", fmt.Errorf("failed to upload file to R2: %w", err)
	}

	fileURL := fmt.Sprintf("%s/%s", r.publicURL, fileName)
	return fileURL, nil
}
